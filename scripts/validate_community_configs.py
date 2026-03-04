#!/usr/bin/env python3
"""Validate community configuration files for ChooseMeal."""

from __future__ import annotations

import argparse
import json
import re
import sys
from pathlib import Path
from typing import Any

DATE_RE = re.compile(r"^\d{4}-\d{2}-\d{2}$")
CONFIG_GLOB = "*.json"


def load_json(path: Path, errors: list[str]) -> dict[str, Any] | None:
    try:
        with path.open("r", encoding="utf-8-sig") as f:
            data = json.load(f)
    except FileNotFoundError:
        errors.append(f"{path}: file not found")
        return None
    except json.JSONDecodeError as exc:
        errors.append(f"{path}: invalid JSON ({exc})")
        return None

    if not isinstance(data, dict):
        errors.append(f"{path}: root must be a JSON object")
        return None
    return data


def is_non_empty_string(value: Any) -> bool:
    return isinstance(value, str) and value.strip() != ""


def is_int(value: Any) -> bool:
    return isinstance(value, int) and not isinstance(value, bool)


def validate_choosemeal_config(path: Path, errors: list[str]) -> bool:
    doc = load_json(path, errors)
    if doc is None:
        return False

    required = ["version", "cafeterias", "floors", "meals"]
    missing = [key for key in required if key not in doc]
    if missing:
        errors.append(f"{path}: missing keys {missing}")
        return False

    if doc["version"] != 1:
        errors.append(f"{path}: version must be 1")

    cafeterias = doc["cafeterias"]
    floors = doc["floors"]
    meals = doc["meals"]

    if not isinstance(cafeterias, list):
        errors.append(f"{path}: cafeterias must be an array")
        return False
    if not isinstance(floors, list):
        errors.append(f"{path}: floors must be an array")
        return False
    if not isinstance(meals, list):
        errors.append(f"{path}: meals must be an array")
        return False

    cafeteria_ids: set[int] = set()
    floor_ids: set[int] = set()
    meal_ids: set[int] = set()

    for idx, cafeteria in enumerate(cafeterias):
        prefix = f"{path}: cafeterias[{idx}]"
        if not isinstance(cafeteria, dict):
            errors.append(f"{prefix} must be an object")
            continue
        for key in ["id", "name", "sortOrder", "enabled"]:
            if key not in cafeteria:
                errors.append(f"{prefix} missing key '{key}'")
        if not is_int(cafeteria.get("id")):
            errors.append(f"{prefix}.id must be an integer")
        else:
            cid = cafeteria["id"]
            if cid in cafeteria_ids:
                errors.append(f"{path}: duplicate cafeteria id {cid}")
            cafeteria_ids.add(cid)
        if not is_non_empty_string(cafeteria.get("name")):
            errors.append(f"{prefix}.name must be a non-empty string")

    for idx, floor in enumerate(floors):
        prefix = f"{path}: floors[{idx}]"
        if not isinstance(floor, dict):
            errors.append(f"{prefix} must be an object")
            continue
        for key in ["id", "cafeteriaId", "name", "sortOrder", "enabled"]:
            if key not in floor:
                errors.append(f"{prefix} missing key '{key}'")
        if not is_int(floor.get("id")):
            errors.append(f"{prefix}.id must be an integer")
        else:
            fid = floor["id"]
            if fid in floor_ids:
                errors.append(f"{path}: duplicate floor id {fid}")
            floor_ids.add(fid)
        if not is_int(floor.get("cafeteriaId")):
            errors.append(f"{prefix}.cafeteriaId must be an integer")
        elif floor["cafeteriaId"] not in cafeteria_ids:
            errors.append(f"{prefix}.cafeteriaId references missing cafeteria id {floor['cafeteriaId']}")
        if not is_non_empty_string(floor.get("name")):
            errors.append(f"{prefix}.name must be a non-empty string")

    for idx, meal in enumerate(meals):
        prefix = f"{path}: meals[{idx}]"
        if not isinstance(meal, dict):
            errors.append(f"{prefix} must be an object")
            continue
        for key in ["id", "floorId", "name", "tags", "enabled"]:
            if key not in meal:
                errors.append(f"{prefix} missing key '{key}'")
        if not is_int(meal.get("id")):
            errors.append(f"{prefix}.id must be an integer")
        else:
            mid = meal["id"]
            if mid in meal_ids:
                errors.append(f"{path}: duplicate meal id {mid}")
            meal_ids.add(mid)
        if not is_int(meal.get("floorId")):
            errors.append(f"{prefix}.floorId must be an integer")
        elif meal["floorId"] not in floor_ids:
            errors.append(f"{prefix}.floorId references missing floor id {meal['floorId']}")
        if not is_non_empty_string(meal.get("name")):
            errors.append(f"{prefix}.name must be a non-empty string")
        if not isinstance(meal.get("tags"), str):
            errors.append(f"{prefix}.tags must be a string")

    return True


def validate_index(root: Path, index_path: Path, config_paths: list[Path], errors: list[str]) -> tuple[int, set[Path]]:
    index_doc = load_json(index_path, errors)
    if index_doc is None:
        return 0, set()

    required = ["version", "updatedAt", "configs"]
    missing = [key for key in required if key not in index_doc]
    if missing:
        errors.append(f"{index_path}: missing keys {missing}")
        return 0, set()

    if index_doc["version"] != 1:
        errors.append(f"{index_path}: version must be 1")

    if not is_non_empty_string(index_doc.get("updatedAt")) or not DATE_RE.match(index_doc["updatedAt"]):
        errors.append(f"{index_path}: updatedAt must match YYYY-MM-DD")

    configs = index_doc["configs"]
    if not isinstance(configs, list):
        errors.append(f"{index_path}: configs must be an array")
        return 0, set()

    entry_ids: set[str] = set()
    indexed_files: set[Path] = set()

    for idx, entry in enumerate(configs):
        prefix = f"{index_path}: configs[{idx}]"
        if not isinstance(entry, dict):
            errors.append(f"{prefix} must be an object")
            continue

        for key in ["id", "schoolName", "city", "file", "author", "updatedAt", "tags"]:
            if key not in entry:
                errors.append(f"{prefix} missing key '{key}'")

        entry_id = entry.get("id")
        if not is_non_empty_string(entry_id):
            errors.append(f"{prefix}.id must be a non-empty string")
        else:
            if entry_id in entry_ids:
                errors.append(f"{index_path}: duplicate config id '{entry_id}'")
            entry_ids.add(entry_id)

        for key in ["schoolName", "city", "author"]:
            if not is_non_empty_string(entry.get(key)):
                errors.append(f"{prefix}.{key} must be a non-empty string")

        if not is_non_empty_string(entry.get("updatedAt")) or not DATE_RE.match(entry["updatedAt"]):
            errors.append(f"{prefix}.updatedAt must match YYYY-MM-DD")

        tags = entry.get("tags")
        if not isinstance(tags, list) or any(not is_non_empty_string(tag) for tag in tags):
            errors.append(f"{prefix}.tags must be an array of non-empty strings")

        file_field = entry.get("file")
        if not is_non_empty_string(file_field):
            errors.append(f"{prefix}.file must be a non-empty string")
            continue
        if not file_field.startswith("community/configs/") or not file_field.endswith(".json"):
            errors.append(f"{prefix}.file must be under community/configs and end with .json")
            continue

        file_path = (root / file_field).resolve()
        try:
            relative_to_root = file_path.relative_to(root.resolve())
        except ValueError:
            errors.append(f"{prefix}.file points outside repository")
            continue

        if not file_path.exists():
            errors.append(f"{prefix}.file not found: {relative_to_root}")
            continue
        indexed_files.add(file_path)

    config_set = {path.resolve() for path in config_paths}
    missing_in_index = sorted(config_set - indexed_files)
    missing_on_disk = sorted(indexed_files - config_set)

    for path in missing_in_index:
        rel = path.relative_to(root.resolve())
        errors.append(f"{index_path}: config file not indexed: {rel}")

    for path in missing_on_disk:
        rel = path.relative_to(root.resolve())
        errors.append(f"{index_path}: indexed file is not in community/configs scan: {rel}")

    return len(configs), indexed_files


def main() -> int:
    parser = argparse.ArgumentParser(description="Validate ChooseMeal community JSON files")
    parser.add_argument("--project-root", default=".", help="Path to repository root")
    args = parser.parse_args()

    root = Path(args.project_root).resolve()
    community_root = root / "community"
    configs_root = community_root / "configs"
    index_path = community_root / "index.json"

    errors: list[str] = []

    if not community_root.exists():
        print(f"[ERROR] community directory not found: {community_root}")
        return 1
    if not configs_root.exists():
        print(f"[ERROR] configs directory not found: {configs_root}")
        return 1

    config_paths = sorted(path for path in configs_root.rglob(CONFIG_GLOB) if path.is_file())
    if not config_paths:
        errors.append(f"{configs_root}: no config JSON files found")

    validated_count = 0
    for config_path in config_paths:
        if validate_choosemeal_config(config_path, errors):
            validated_count += 1

    index_count, _ = validate_index(root, index_path, config_paths, errors)

    if errors:
        print("[FAIL] Community config validation failed:")
        for err in errors:
            print(f"- {err}")
        return 1

    print("[PASS] Community config validation passed")
    print(f"- Config files validated: {validated_count}")
    print(f"- Index entries: {index_count}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
