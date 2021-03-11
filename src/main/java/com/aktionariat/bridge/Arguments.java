package com.aktionariat.bridge;

import java.util.Arrays;

public class Arguments {

	private String[] args;

	public Arguments(String[] args) {
		this.args = args;
	}

	public boolean hasArgument(String string) {
		return index(string) >= 0;
	}
	
	public int get(String key, int def) {
		String arg = get(key);
		if (arg == null) {
			return def;
		} else {
			return Integer.parseInt(arg);
		}
	}

	public String get(String arg) {
		int index = index(arg);
		if (index >= 0 && args.length > index + 1) {
			return args[index + 1];
		} else {
			return null;
		}
	}
	
	private int index(String arg) {
		for (int i=0; i<args.length; i++) {
			if (args[i].contentEquals(arg)) {
				return i;
			}
		}
		return -1;
	}

	@Override
	public String toString() {
		return Arrays.toString(args);
	}
	
	public static void main(String[] args) {
		Arguments arg = new Arguments(args);
		System.out.println(arg.get("-host"));
	}

}
