import { Hex as HexType } from '../types/game';

export class Hex implements HexType {
  public readonly q: number;
  public readonly r: number;

  static readonly DIRECTIONS = [
    [1, 0], [1, -1], [0, -1],
    [-1, 0], [-1, 1], [0, 1]
  ];

  constructor(q: number, r: number) {
    this.q = q;
    this.r = r;
  }

  equals(other: Hex): boolean {
    return this.q === other.q && this.r === other.r;
  }

  toString(): string {
    return `Hex(${this.q}, ${this.r})`;
  }

  static fromString(str: string): Hex | null {
    if (!str || str.trim() === '') return null;

    try {
      const clean = str.trim();
      if (clean.startsWith('Hex(') && clean.endsWith(')')) {
        const coords = clean.substring(4, clean.length - 1);
        const parts = coords.split(',');
        if (parts.length === 2) {
          const q = parseInt(parts[0].trim());
          const r = parseInt(parts[1].trim());
          return new Hex(q, r);
        }
      }
    } catch (e) {
      // Return null on parse error
    }
    return null;
  }

  distance(other: Hex): number {
    return (Math.abs(this.q - other.q) + Math.abs(this.q + this.r - other.q - other.r) + Math.abs(this.r - other.r)) / 2;
  }

  neighbor(direction: number): Hex {
    const dir = Hex.DIRECTIONS[direction];
    return new Hex(this.q + dir[0], this.r + dir[1]);
  }

  add(other: Hex): Hex {
    return new Hex(this.q + other.q, this.r + other.r);
  }

  subtract(other: Hex): Hex {
    return new Hex(this.q - other.q, this.r - other.r);
  }

  toPixel(centerX: number, centerY: number, hexSize: number): [number, number] {
    const x = hexSize * (3 / 2 * this.q);
    const y = hexSize * (Math.sqrt(3) / 2 * this.q + Math.sqrt(3) * this.r);
    return [centerX + x, centerY + y];
  }

  static fromPixel(x: number, y: number, centerX: number, centerY: number, hexSize: number): Hex {
    x -= centerX;
    y -= centerY;

    const q = (2 / 3 * x) / hexSize;
    const r = (-1 / 3 * x + Math.sqrt(3) / 3 * y) / hexSize;

    return Hex.roundHex(q, r);
  }

  private static roundHex(q: number, r: number): Hex {
    const s = -q - r;
    let rq = Math.round(q);
    let rr = Math.round(r);
    const rs = Math.round(s);

    const qDiff = Math.abs(rq - q);
    const rDiff = Math.abs(rr - r);
    const sDiff = Math.abs(rs - s);

    if (qDiff > rDiff && qDiff > sDiff) {
      rq = -rr - rs;
    } else if (rDiff > sDiff) {
      rr = -rq - rs;
    }

    return new Hex(rq, rr);
  }
}