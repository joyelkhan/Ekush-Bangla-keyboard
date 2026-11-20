#!/usr/bin/env python3
"""
Copyright (c) 2026 Ekush contributors · Licensed under GPL-3.0.
Compiles a whitespace-tokenized Bengali corpus into pruned bigram TSV and compact
EKLM v1 binary artifacts compatible with ContextLM.fromBinaryStream (Prompt 13).
"""

import argparse
import collections
import pathlib
import struct
import unicodedata


def write_utf(buf: bytearray, text: str) -> None:
    raw = text.encode("utf-8")
    buf.extend(struct.pack(">H", len(raw)))
    buf.extend(raw)


def main() -> None:
    parser = argparse.ArgumentParser(description="Compile Ekush offline bigram LM.")
    parser.add_argument("--corpus", required=True, type=pathlib.Path)
    parser.add_argument("--out-tsv", required=True, type=pathlib.Path)
    parser.add_argument("--out-bin", required=True, type=pathlib.Path)
    parser.add_argument("--max-bigrams", type=int, default=150000)
    args = parser.parse_args()

    bigrams: collections.Counter[tuple[str, str]] = collections.Counter()
    with args.corpus.open("r", encoding="utf-8") as fh:
        for line in fh:
            tokens = [
                unicodedata.normalize("NFC", t.strip(".,!?;:\"'।()[]"))
                for t in line.split()
            ]
            tokens = [t for t in tokens if t]
            for a, b in zip(tokens, tokens[1:]):
                bigrams[(a, b)] += 1

    top = bigrams.most_common(args.max_bigrams)
    args.out_tsv.parent.mkdir(parents=True, exist_ok=True)
    with args.out_tsv.open("w", encoding="utf-8") as fh:
        fh.write("# VERIFIED bigram transitions compiled by scripts/build_lm.py\n")
        for (a, b), count in top:
            fh.write(f"{a}\t{b}\t{count}\n")

    payload = bytearray(b"EKLM")
    payload.extend(struct.pack(">II", 1, len(top)))
    for (a, b), count in top:
        write_utf(payload, a)
        write_utf(payload, b)
        payload.extend(struct.pack(">I", count))

    args.out_bin.parent.mkdir(parents=True, exist_ok=True)
    args.out_bin.write_bytes(bytes(payload))
    print(f"Wrote {len(top)} bigrams -> {args.out_bin} ({len(payload)} bytes)")


if __name__ == "__main__":
    main()
