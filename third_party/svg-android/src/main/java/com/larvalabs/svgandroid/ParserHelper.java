package com.larvalabs.svgandroid;

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements. See the NOTICE
 * file distributed with this work for additional information regarding copyright ownership. The ASF licenses this file
 * to You under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0 Unless required by
 * applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
/**
 * Parses numbers from SVG text. Based on the Batik Number Parser (Apache 2 License).
 * 
 * @author Apache Software Foundation, Larva Labs LLC
 */
public class ParserHelper {

	private char current;
	private String s;
	public int pos;
	private int n;
	
	public ParserHelper(String str, int pos) {
		this.s = str;
		this.pos = pos;
		n = str.length();
		current = str.charAt(pos);
	}

	private char read() {
		if (pos < n) {
			pos++;
		}
		if (pos == n) {
			return '\0';
		} else {
			return s.charAt(pos);
		}
	}

	public void skipWhitespace() {
		while (pos < n) {
			if (Character.isWhitespace(s.charAt(pos))) {
				advance();
			} else {
				break;
			}
		}
	}

	public void skipNumberSeparator() {
		while (pos < n) {
			char c = s.charAt(pos);
			switch (c) {
			case ' ':
			case ',':
			case '\n':
			case '\t':
				advance();
				break;
			default:
				return;
			}
		}
	}

	public void advance() {
		current = read();
	}

	/**
	 * Parses the content of the buffer and converts it to a float.
	 */
	public float parseFloat() {
		int mant = 0;
		int mantDig = 0;
		boolean mantPos = true;
		boolean mantRead = false;

		int exp = 0;
		int expDig = 0;
		int expAdj = 0;
		boolean expPos = true;

		switch (current) {
		case '-':
			mantPos = false;
			// fallthrough
		case '+':
			current = read();
		}

		m1: switch (current) {
		default:
			return Float.NaN;

		case '.':
			break;

		case '0':
			mantRead = true;
			l: for (;;) {
				current = read();
				switch (current) {
				case '1':
				case '2':
				case '3':
				case '4':
				case '5':
				case '6':
				case '7':
				case '8':
				case '9':
					break l;
				case '.':
				case 'e':
				case 'E':
					break m1;
				default:
					return 0.0f;
				case '0':
				}
			}

		case '1':
		case '2':
		case '3':
		case '4':
		case '5':
		case '6':
		case '7':
		case '8':
		case '9':
			mantRead = true;
			l: for (;;) {
				if (mantDig < 9) {
					mantDig++;
					mant = mant * 10 + (current - '0');
				} else {
					expAdj++;
				}
				current = read();
				switch (current) {
				default:
					break l;
				case '0':
				case '1':
				case '2':
				case '3':
				case '4':
				case '5':
				case '6':
				case '7':
				case '8':
				case '9':
				}
			}
		}

		if (current == '.') {
			current = read();
			m2: switch (current) {
			default:
			case 'e':
			case 'E':
				if (!mantRead) {
					reportUnexpectedCharacterError(current);
					return 0.0f;
				}
				break;

			case '0':
				if (mantDig == 0) {
					l: for (;;) {
						current = read();
						expAdj--;
						switch (current) {
						case '1':
						case '2':
						case '3':
						case '4':
						case '5':
						case '6':
						case '7':
						case '8':
						case '9':
							break l;
						default:
							if (!mantRead) {
								return 0.0f;
							}
							break m2;
						case '0':
						}
					}
				}
			case '1':
			case '2':
			case '3':
			case '4':
			case '5':
			case '6':
			case '7':
			case '8':
			case '9':
				l: for (;;) {
					if (mantDig < 9) {
						mantDig++;
						mant = mant * 10 + (current - '0');
						expAdj--;
					}
					current = read();
					switch (current) {
					default:
						break l;
					case '0':
					case '1':
					case '2':
					case '3':
					case '4':
					case '5':
					case '6':
					case '7':
					case '8':
					case '9':
					}
				}
			}
		}

		switch (current) {
		case 'e':
		case 'E':
			current = read();
			switch (current) {
			default:
				reportUnexpectedCharacterError(current);
				return 0f;
			case '-':
				expPos = false;
			case '+':
				current = read();
				switch (current) {
				default:
					reportUnexpectedCharacterError(current);
					return 0f;
				case '0':
				case '1':
				case '2':
				case '3':
				case '4':
				case '5':
				case '6':
				case '7':
				case '8':
				case '9':
				}
			case '0':
			case '1':
			case '2':
			case '3':
			case '4':
			case '5':
			case '6':
			case '7':
			case '8':
			case '9':
			}

			en: switch (current) {
			case '0':
				l: for (;;) {
					current = read();
					switch (current) {
					case '1':
					case '2':
					case '3':
					case '4':
					case '5':
					case '6':
					case '7':
					case '8':
					case '9':
						break l;
					default:
						break en;
					case '0':
					}
				}

			case '1':
			case '2':
			case '3':
			case '4':
			case '5':
			case '6':
			case '7':
			case '8':
			case '9':
				l: for (;;) {
					if (expDig < 3) {
						expDig++;
						exp = exp * 10 + (current - '0');
					}
					current = read();
					switch (current) {
					default:
						break l;
					case '0':
					case '1':
					case '2':
					case '3':
					case '4':
					case '5':
					case '6':
					case '7':
					case '8':
					case '9':
					}
				}
			}
		default:
		}

		if (!expPos) {
			exp = -exp;
		}
		exp += expAdj;
		if (!mantPos) {
			mant = -mant;
		}

		return buildFloat(mant, exp);
	}

	private void reportUnexpectedCharacterError(char c) {
		throw new RuntimeException("Unexpected char '" + c + "'.");
	}

	/**
	 * Computes a float from mantissa and exponent.
	 */
	public static float buildFloat(int mant, int exp) {
		if (exp < -125 || mant == 0) {
			return 0.0f;
		}

		if (exp >= 128) {
			return (mant > 0) ? Float.POSITIVE_INFINITY : Float.NEGATIVE_INFINITY;
		}

		if (exp == 0) {
			return mant;
		}

		if (mant >= (1 << 26)) {
			mant++; // round up trailing bits if they will be dropped.
		}

		return (float) ((exp > 0) ? mant * pow10[exp] : mant / pow10[-exp]);
	}

	/**
	 * Array of powers of ten. Using double instead of float gives a tiny bit more precision.
	 */
	private static final double[] pow10 = new double[128];

	static {
		for (int i = 0; i < pow10.length; i++) {
			pow10[i] = Math.pow(10, i);
		}
	}

	public float nextFloat() {
		skipWhitespace();
		float f = parseFloat();
		skipNumberSeparator();
		return f;
	}

	public int nextFlag() {
		skipWhitespace();
		int flag = current - '0';
		current = read();
		skipNumberSeparator();
		return flag;
	}

}
