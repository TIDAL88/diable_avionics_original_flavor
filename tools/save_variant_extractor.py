#!/usr/bin/env python3
"""
Extract Starsector .variant JSON files from a campaign.xml save.

Default behavior is intentionally safe:
  - reads one campaign.xml;
  - targets the save's player fleet unless a fleet reference/name is supplied;
  - writes to out/extracted_variants;
  - refuses to overwrite existing files unless --overwrite is passed.

Important save mapping:
  savedVariant attribute v -> fluxVents
  savedVariant attribute c -> fluxCapacitors
"""

from __future__ import annotations

import argparse
import json
import re
import sys
import xml.etree.ElementTree as ET
from collections import Counter
from pathlib import Path
from typing import Any


DEFAULT_OUTPUT_DIR = Path("out/extracted_variants")
DEFAULT_ID_TEMPLATE = "{prefix}{index:02d}_{hull}"


def parse_bool(value: str | None, default: bool = False) -> bool:
    if value is None:
        return default
    return value.lower() == "true"


def parse_int(value: str | None, default: int = 0) -> int:
    if value is None:
        return default
    return int(value)


def sanitize_identifier(value: str | None, fallback: str) -> str:
    value = value or fallback
    value = value.strip().lower()
    value = re.sub(r"[^a-z0-9_]+", "_", value)
    value = re.sub(r"_+", "_", value).strip("_")
    return value or fallback


def direct_st_texts(parent: ET.Element | None, child_tag: str) -> list[str]:
    if parent is None:
        return []
    child = parent.find(child_tag)
    if child is None:
        return []
    return [
        st.text
        for st in child.findall("st")
        if st.text is not None and st.text != ""
    ]


def parse_weapon_map(saved_variant: ET.Element) -> dict[str, str]:
    weapons: dict[str, str] = {}
    weapon_container = saved_variant.find("wpn")
    if weapon_container is None:
        return weapons

    for entry in weapon_container.findall("e"):
        fields = entry.findall("st")
        if len(fields) < 2:
            continue
        slot = fields[0].text
        weapon_id = fields[1].text
        if slot and weapon_id:
            weapons[slot] = weapon_id
    return weapons


def parse_weapon_groups(
    saved_variant: ET.Element,
    weapons: dict[str, str],
) -> tuple[list[dict[str, Any]], list[str]]:
    groups: list[dict[str, Any]] = []
    grouped_slots: set[str] = set()
    weapon_groups = saved_variant.find("wG")

    if weapon_groups is not None:
        for spec in weapon_groups.findall("WGSpec"):
            slots_parent = spec.find("s")
            slots = direct_st_texts(spec, "s") if slots_parent is not None else []
            group_weapons = {
                slot: weapons[slot]
                for slot in slots
                if slot in weapons
            }
            grouped_slots.update(group_weapons.keys())

            if group_weapons:
                groups.append(
                    {
                        "autofire": parse_bool(spec.get("a")),
                        "mode": spec.get("t") or "LINKED",
                        "weapons": group_weapons,
                    }
                )

    ungrouped_slots = [
        slot
        for slot in weapons.keys()
        if slot not in grouped_slots
    ]
    if ungrouped_slots:
        groups.append(
            {
                "autofire": False,
                "mode": "LINKED",
                "weapons": {
                    slot: weapons[slot]
                    for slot in ungrouped_slots
                },
            }
        )

    return groups, ungrouped_slots


def find_save_root(save_path: Path) -> ET.Element:
    try:
        return ET.parse(save_path).getroot()
    except ET.ParseError as exc:
        raise SystemExit(f"Could not parse XML save: {save_path}\n{exc}") from exc


def find_fleets(root: ET.Element) -> list[ET.Element]:
    return [
        element
        for element in root.iter()
        if is_fleet_element(element)
    ]


def is_fleet_element(element: ET.Element) -> bool:
    return element.tag == "Flt" or element.get("cl") == "Flt"


def fleet_ref(fleet: ET.Element) -> str | None:
    return fleet.get("z")


def fleet_name(fleet: ET.Element) -> str:
    return fleet.get("n") or ""


def fleet_faction(fleet: ET.Element) -> str:
    fleet_data = fleet.find("fD")
    if fleet_data is None:
        return ""
    return fleet_data.get("nSf") or ""


def fleet_members(fleet: ET.Element) -> list[ET.Element]:
    fleet_data = fleet.find("fD")
    if fleet_data is None:
        return []
    member_container = fleet_data.find("m")
    if member_container is None:
        return []
    return member_container.findall("FMmbr")


def list_fleets(root: ET.Element) -> None:
    rows: list[tuple[str, str, str, int, int]] = []
    for fleet in find_fleets(root):
        members = fleet_members(fleet)
        saved_variants = sum(
            1
            for member in members
            if member.find("savedVariant") is not None
        )
        if not members and not saved_variants:
            continue
        rows.append(
            (
                fleet_ref(fleet) or "",
                fleet_name(fleet),
                fleet_faction(fleet),
                len(members),
                saved_variants,
            )
        )

    print("fleet_ref\tname\tfaction\tmembers\tsaved_variants")
    for row in rows:
        print("\t".join(str(value) for value in row))


def player_fleet_ref(root: ET.Element) -> str | None:
    player_fleet = root.find("playerFleet")
    if player_fleet is None:
        return None
    return player_fleet.get("ref")


def find_fleet(root: ET.Element, args: argparse.Namespace) -> ET.Element:
    fleets = find_fleets(root)

    if args.fleet_ref:
        for fleet in fleets:
            if fleet_ref(fleet) == args.fleet_ref:
                return fleet
        raise SystemExit(f"No fleet found with z/ref {args.fleet_ref}")

    if args.fleet_name:
        matches = [
            fleet
            for fleet in fleets
            if fleet_name(fleet) == args.fleet_name
        ]
        if len(matches) == 1:
            return matches[0]
        if not matches:
            raise SystemExit(f"No fleet found named {args.fleet_name!r}")
        refs = ", ".join(fleet_ref(fleet) or "?" for fleet in matches)
        raise SystemExit(
            f"Multiple fleets named {args.fleet_name!r}; use --fleet-ref. "
            f"Matches: {refs}"
        )

    ref = player_fleet_ref(root)
    if ref is None:
        raise SystemExit(
            "No --fleet-ref/--fleet-name supplied and save has no playerFleet ref"
        )
    for fleet in fleets:
        if fleet_ref(fleet) == ref:
            return fleet
    raise SystemExit(f"Save playerFleet ref {ref} does not resolve to a fleet")


def member_is_civilian(member: ET.Element) -> bool:
    return parse_bool(member.get("civ")) or parse_bool(member.get("cCiv"))


def member_sortable_name(member: ET.Element, fallback: str) -> str:
    return member.get("sN") or member.get("sid") or fallback


def build_variant_id(
    member: ET.Element,
    saved_variant: ET.Element,
    index: int,
    args: argparse.Namespace,
    used_ids: Counter[str],
) -> str:
    hull = sanitize_identifier(saved_variant.get("hId"), "hull")
    ship = sanitize_identifier(member.get("sN"), hull)
    sid = sanitize_identifier(member.get("sid"), hull)
    base = args.id_template.format(
        prefix=args.prefix,
        index=index,
        hull=hull,
        ship=ship,
        sid=sid,
    )
    variant_id = sanitize_identifier(base, f"{args.prefix}{index:02d}")
    used_ids[variant_id] += 1
    if used_ids[variant_id] > 1:
        variant_id = f"{variant_id}_{used_ids[variant_id]}"
    return variant_id


def convert_saved_variant(
    member: ET.Element,
    saved_variant: ET.Element,
    variant_id: str,
    display_name: str | None,
    strip_smods: bool,
) -> tuple[dict[str, Any], list[str], list[str]]:
    weapons = parse_weapon_map(saved_variant)
    weapon_groups, ungrouped_slots = parse_weapon_groups(saved_variant, weapons)

    hull_mods = direct_st_texts(saved_variant, "hM")
    perma_mods = direct_st_texts(saved_variant, "pM")
    s_mods = direct_st_texts(saved_variant, "sMods")
    s_modded_built_ins = direct_st_texts(saved_variant, "sModdedBuiltIns")
    tags = direct_st_texts(saved_variant, "tags")
    wings = direct_st_texts(saved_variant, "wng")

    stripped_smods: list[str] = []
    if strip_smods:
        stripped_smods = list(s_mods) + list(s_modded_built_ins)
        smod_set = set(s_mods)
        perma_mods = [
            hullmod_id
            for hullmod_id in perma_mods
            if hullmod_id not in smod_set
        ]
        s_mods = []
        s_modded_built_ins = []

    variant: dict[str, Any] = {
        "displayName": display_name
        if display_name is not None
        else saved_variant.get("vDN", "Custom"),
        "fluxCapacitors": parse_int(saved_variant.get("c")),
        "fluxVents": parse_int(saved_variant.get("v")),
        "goalVariant": parse_bool(saved_variant.get("gV")),
        "hullId": saved_variant.get("hId"),
        "hullMods": hull_mods,
        "permaMods": perma_mods,
        "sMods": s_mods,
        "sModdedBuiltIns": s_modded_built_ins,
        "variantId": variant_id,
        "weaponGroups": weapon_groups,
        "wings": wings,
    }

    if tags:
        variant["tags"] = tags

    return variant, ungrouped_slots, stripped_smods


def write_variant(
    variant: dict[str, Any],
    output_dir: Path,
    overwrite: bool,
) -> Path:
    output_dir.mkdir(parents=True, exist_ok=True)
    path = output_dir / f"{variant['variantId']}.variant"
    if path.exists() and not overwrite:
        raise SystemExit(
            f"Refusing to overwrite existing file: {path}\n"
            f"Pass --overwrite or choose a different --out/--prefix."
        )
    with path.open("w", encoding="utf-8") as handle:
        json.dump(variant, handle, indent=4)
        handle.write("\n")
    return path


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Extract .variant files from a Starsector campaign.xml save."
    )
    parser.add_argument(
        "save",
        type=Path,
        help="Path to campaign.xml",
    )
    parser.add_argument(
        "--list-fleets",
        action="store_true",
        help="List fleets with saved variants and exit.",
    )
    parser.add_argument(
        "--fleet-ref",
        help="Fleet z/ref to extract. Defaults to the save's playerFleet ref.",
    )
    parser.add_argument(
        "--fleet-name",
        help="Exact fleet name to extract. If ambiguous, use --fleet-ref.",
    )
    parser.add_argument(
        "--out",
        type=Path,
        default=DEFAULT_OUTPUT_DIR,
        help=f"Output folder. Default: {DEFAULT_OUTPUT_DIR}",
    )
    parser.add_argument(
        "--prefix",
        default="exported_",
        help="Variant ID prefix. Default: exported_",
    )
    parser.add_argument(
        "--id-template",
        default=DEFAULT_ID_TEMPLATE,
        help=(
            "Variant ID template. Tokens: {prefix}, {index}, {hull}, {ship}, "
            f"{{sid}}. Default: {DEFAULT_ID_TEMPLATE}"
        ),
    )
    parser.add_argument(
        "--display-name",
        help="Force this displayName for all exported variants.",
    )
    parser.add_argument(
        "--exclude-civilian",
        action="store_true",
        help="Skip members marked civ/cCiv in the save.",
    )
    parser.add_argument(
        "--strip-smods",
        action="store_true",
        help=(
            "Write console-safe variants: clear sMods/sModdedBuiltIns and "
            "remove sMods from permaMods. Exact mode is the default."
        ),
    )
    parser.add_argument(
        "--overwrite",
        action="store_true",
        help="Allow replacing existing output files.",
    )
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    save_path = args.save.expanduser().resolve()
    if not save_path.exists():
        raise SystemExit(f"Save file not found: {save_path}")

    root = find_save_root(save_path)

    if args.list_fleets:
        list_fleets(root)
        return 0

    fleet = find_fleet(root, args)
    members = fleet_members(fleet)
    if args.exclude_civilian:
        members = [
            member
            for member in members
            if not member_is_civilian(member)
        ]

    output_dir = args.out
    if not output_dir.is_absolute():
        output_dir = Path.cwd() / output_dir

    used_ids: Counter[str] = Counter()
    rows: list[dict[str, Any]] = []
    exported_count = 0

    for index, member in enumerate(members, start=1):
        saved_variant = member.find("savedVariant")
        if saved_variant is None:
            continue

        variant_id = build_variant_id(
            member,
            saved_variant,
            index,
            args,
            used_ids,
        )
        variant, ungrouped_slots, stripped_smods = convert_saved_variant(
            member,
            saved_variant,
            variant_id,
            args.display_name,
            args.strip_smods,
        )
        path = write_variant(variant, output_dir, args.overwrite)
        exported_count += 1

        rows.append(
            {
                "index": index,
                "ship": member_sortable_name(member, variant["hullId"]),
                "variant": variant_id,
                "hull": variant["hullId"],
                "vents": variant["fluxVents"],
                "caps": variant["fluxCapacitors"],
                "weapons": sum(
                    len(group["weapons"])
                    for group in variant["weaponGroups"]
                ),
                "wings": len(variant["wings"]),
                "perma": ",".join(variant["permaMods"]),
                "smods": ",".join(stripped_smods or variant["sMods"]),
                "ungrouped": ",".join(ungrouped_slots),
                "file": str(path),
            }
        )

    print(
        f"Extracted {exported_count} variants from fleet "
        f"{fleet_ref(fleet) or '?'} ({fleet_name(fleet) or 'unnamed'})"
    )
    print(f"Output: {output_dir}")
    print()
    print(
        "idx\tship\tvariantId\thull\tvents\tcaps\tweapons\twings\t"
        "permaMods\tsMods_or_stripped\tungrouped_weapon_slots"
    )
    for row in rows:
        print(
            "\t".join(
                str(row[key])
                for key in (
                    "index",
                    "ship",
                    "variant",
                    "hull",
                    "vents",
                    "caps",
                    "weapons",
                    "wings",
                    "perma",
                    "smods",
                    "ungrouped",
                )
            )
        )

    return 0


if __name__ == "__main__":
    raise SystemExit(main())
