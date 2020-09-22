#!/usr/bin/env python3

OWNER = 1
DESCRIPTION_START = "test"
TOKEN_START = "01234567890123456789"
TOKEN_PAD = "a"

def generate_inserts(f, N0, N1, owner=OWNER, unit="V", token_start=TOKEN_START, token_pad=TOKEN_PAD):
    f.write("INSERT IGNORE INTO objects (id, name, ugrp_id, unit_symbol) VALUES\n")
    objects = [f'    ({i}, "{DESCRIPTION_START} object {i}", {owner}, "{unit}")' for i in range(N0, N1+1)]
    f.write(',\n'.join(objects) + ";")

    f.write('\n\n')

    def token(i):
        s = str(i)
        pad_length = 32 - len(TOKEN_START + s)
        return TOKEN_START + (token_pad * pad_length) + s

    f.write("INSERT IGNORE INTO tokens (token, object_id, description) VALUES\n")
    tokens = [f'    ("{token(i)}", {i}, "{DESCRIPTION_START} token {i}")' for i in range(N0, N1+1)]
    f.write(',\n'.join(tokens) + ";")

    f.write('\n')

if __name__ == '__main__':
    import argparse
    parser = argparse.ArgumentParser()
    parser.add_argument('-n0', default=1, type=int, help="first object_id, inclusive")
    parser.add_argument('-n1', default=2, type=int, help="last object_id, inclusive")
    parser.add_argument('-f', default='-', type=argparse.FileType('w'), help="output file")

    args = parser.parse_args()

    generate_inserts(args.f, args.n0, args.n1)

