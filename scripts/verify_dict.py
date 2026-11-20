#!/usr/bin/env python3
"""
Copyright (c) 2026 Ekush contributors · Licensed under GPL-3.0.
Cross-checks a shipped starter dictionary against a verified corpus TSV, marking matches
VERIFIED or DROPPED and writing an updated TSV report (Prompt 12).
"""

import argparse
import pathlib
import unicodedata


def load_tsv(path: pathlib.Path) -> dict[str, int]:
    rows: dict[str, int] = {}
    for line in path.read_text(encoding="utf-8").splitlines():
        s = line.strip()
        if not s or s.startswith("#"):
            continue
        parts = s.split("\t")
        w = unicodedata.normalize("NFC", parts[0].strip())
        f = int(parts[1]) if len(parts) > 1 and parts[1].isdigit() else 1
        rows[w] = max(rows.get(w, 0), f)
    return rows


def main() -> None:
    parser = argparse.ArgumentParser(description="Verify Ekush starter dictionary against corpus.")
    parser.add_argument("--starter", required=True, type=pathlib.Path)
    parser.add_argument("--corpus", required=True, type=pathlib.Path)
    parser.add_argument("--out", required=True, type=pathlib.Path)
    args = parser.parse_args()

    starter = load_tsv(args.starter)
    corpus = load_tsv(args.corpus)

    verified = []
    dropped = []
    for word in starter:
        if word in corpus:
            verified.append((word, corpus[word]))
        else:
            dropped.append(word)

    verified.sort(key=lambda kv: (-kv[1], kv[0]))
    args.out.parent.mkdir(parents=True, exist_ok=True)
    with args.out.open("w", encoding="utf-8") as fh:
        fh.write("# VERIFIED dictionary entries cross-checked against corpus\n")
        for w, f in verified:
            fh.write(f"{w}\t{f}\n")

    print(f"VERIFIED: {len(verified)}, DROPPED: {len(dropped)}")


if __name__ == "__main__":
    main()
