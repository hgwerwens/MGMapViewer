/*
 * Copyright 2017 - 2021 mg4gh
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package mg.mgmap.generic.util.basic;

/**
 * Utility to determine the class name dynamically.
 * This is usually used fog logging purposes.
 */
@SuppressWarnings("unused")
public class NameUtil {
	 
	public static String getCurrentClassName(){
		return new Throwable().getStackTrace()[1].getClassName();
	}
	
	public static String getCurrentMethodName(){
		return new Throwable().getStackTrace()[1].getMethodName();
	}

	public static String context(){
		StackTraceElement ste = new Throwable().getStackTrace()[1];
		return context(ste);
	}
	public static String context(StackTraceElement ste){
		return ste.getClassName()+"."+ste.getMethodName()+"("+ste.getFileName()+":"+ste.getLineNumber()+") ";
	}
	public static String context(int n){
		StackTraceElement ste = new Throwable().getStackTrace()[n];
		return context(ste);
	}

	public static String[] context(int from, int to){
		String[] steArray = new String[to-from+1];
		for (int i=from; i<=to; i++){
			StackTraceElement ste = new Throwable().getStackTrace()[1+i];
			steArray[i] = ste.getClassName()+"."+ste.getMethodName()+"("+ste.getFileName()+":"+ste.getLineNumber()+") ";
		}
		return steArray;
	}


}
