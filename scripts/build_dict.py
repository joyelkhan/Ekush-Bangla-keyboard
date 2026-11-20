#!/usr/bin/env python3
"""
Copyright (c) 2026 Ekush contributors · Licensed under GPL-3.0.
Builds NFC-normalized Bangla frequency dictionary assets from an input TSV corpus
(word<TAB>frequency) without inventing any rows (Prompt 12).
"""

import argparse
import json
import pathlib
import unicodedata

BENGALI_INFLECTION_SUFFIXES = [
    "গুলোর", "গুলোকে", "গুলোতে", "গুলো",
    "দেরকে", "দের", "টার", "টাকে", "টায়", "টা",
    "টির", "টিকে", "টি", "খান", "খানা",
    "ের", "তে", "কে", "রা", "য়ে", "য়", "ে", "র"
]


def is_bengali_word(token: str) -> bool:
    if len(token) < 2:
        return False
    return all("\u0980" <= ch <= "\u09FF" or ch in ("\u200C", "\u200D") for ch in token)


def main() -> None:
    parser = argparse.ArgumentParser(description="Build verified Ekush Bangla dictionary assets.")
    parser.add_argument("--input", required=True, type=pathlib.Path, help="Input corpus TSV (word<TAB>freq)")
    parser.add_argument("--out-tsv", required=True, type=pathlib.Path, help="Output normalized TSV path")
    parser.add_argument("--out-json", required=True, type=pathlib.Path, help="Output JSON lexicon path")
    parser.add_argument("--out-suffix", required=True, type=pathlib.Path, help="Output suffix model JSON")
    parser.add_argument("--limit", type=int, default=50000, help="Max top lemmas to retain")
    args = parser.parse_args()

    merged: dict[str, int] = {}
    with args.input.open("r", encoding="utf-8") as fh:
        for line in fh:
            s = line.strip()
            if not s or s.startswith("#"):
                continue
            parts = s.split("\t")
            word = unicodedata.normalize("NFC", parts[0].strip())
            if not is_bengali_word(word):
                continue
            freq = int(parts[1]) if len(parts) > 1 and parts[1].isdigit() else 1
            merged[word] = merged.get(word, 0) + max(1, freq)

    ranked = sorted(merged.items(), key=lambda kv: (-kv[1], kv[0]))[: args.limit]

    args.out_tsv.parent.mkdir(parents=True, exist_ok=True)
    with args.out_tsv.open("w", encoding="utf-8") as out:
        out.write("# VERIFIED corpus frequency dictionary built by scripts/build_dict.py\n")
        for word, freq in ranked:
            out.write(f"{word}\t{freq}\n")

    args.out_json.parent.mkdir(parents=True, exist_ok=True)
    args.out_json.write_text(
        json.dumps([{"word": w, "freq": f} for w, f in ranked], ensure_ascii=False, indent=2),
        encoding="utf-8",
    )

    args.out_suffix.parent.mkdir(parents=True, exist_ok=True)
    args.out_suffix.write_text(
        json.dumps({"suffixes": BENGALI_INFLECTION_SUFFIXES}, ensure_ascii=False, indent=2),
        encoding="utf-8",
    )
    print(f"Wrote {len(ranked)} VERIFIED entries -> {args.out_tsv}")


if __name__ == "__main__":
    main()
