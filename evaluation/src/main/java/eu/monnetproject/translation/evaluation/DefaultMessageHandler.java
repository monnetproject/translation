/**********************************************************************************
 * Copyright (c) 2011, Monnet Project
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of the Monnet Project nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE MONNET PROJECT BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *********************************************************************************/
package eu.monnetproject.translation.evaluation;

import eu.monnetproject.translation.monitor.*;
import static eu.monnetproject.translation.monitor.Messages.MessageType.*;

public class DefaultMessageHandler implements MessageHandler {
	
	private int tempPrintLines = 0;
	private static final String NL = System.getProperty("line.separator");
	private static final boolean pretty = System.getenv("TERM") != null && System.getenv("TERM").equals("xterm") &&
	  	System.getProperty("nocolorterm") == null;
	private static String RED = pretty ? "\033[0;31m" : "";
	private static String YELLOW = pretty ? "\033[0;33m" : "";
	private static String GREEN = pretty ? "\033[0;32m" : "";
	private static String BLACK = pretty ? "\033[0;00m" : "";
	private static String ERASE_LINE = pretty ? "\u001B[1A\u001B[2K" : "";
	
        public void setPretty(boolean pretty) {
            RED = pretty ? "\033[0;31m" : "";
            YELLOW = pretty ? "\033[0;33m" : "";
            GREEN = pretty ? "\033[0;32m" : "";
            BLACK = pretty ? "\033[0;00m" : "";
            ERASE_LINE = pretty ? "\u001B[1A\u001B[2K" : "";
        }
        
	private void print(String str) {
            synchronized(this) {
		System.out.print(str);
            }
	}
	
	@Override public void beginJob(Job job) {
		tempPrintLines = 0;
	}
	
	@Override public void endJob(Job job) {
		clear();
	}
	
	@Override public synchronized void handle(Messages.Message msg) {
		final String str;
		switch(msg.type) {
		case START:
			str = "["+GREEN+"OK"+BLACK+"] Begin translation " + msg.entity + NL; 
			print(str);
			tempPrintLines++;
			break;
		case SUCCESS:
			clear();
			str = "["+GREEN+"OK"+BLACK+"] Translated " + msg.entity + NL + "  >> " +
			  msg.message;
			print(str+NL);
			break;
		case FAIL:
			clear();
			str = "["+RED+"FAIL"+BLACK+"] Translation of " + msg.entity + " failed due to " +
			   msg.message;
			print(str+NL);
			if(msg.cause != null) {
				msg.cause.printStackTrace();
			}
			break;
		case CLOAD:
			str = "["+GREEN+"OK"+BLACK+"] Loaded " + msg.component.getName() + NL;
			print(str);
			break;
		case CFAIL:
			str = "["+RED+"FAIL"+BLACK+"] Could not load component " + msg.component.getName() + " due to " + msg.message;
			print(str+NL);
			if(msg.cause != null) {
				msg.cause.printStackTrace();
			}
			break;
		case INFO:
			str = "["+GREEN+"INFO"+BLACK+"] " + msg.message + NL;
			print(str);
			tempPrintLines++;
			break;
		case CLEAN_FAIL:
			clear();
			str = "["+RED+"FAIL"+BLACK+"] Error in clean up";
			print(str+NL);
			if(msg.cause != null) {
				msg.cause.printStackTrace();
			}
			break;
		case WARNING:
			clear();
			str = "["+YELLOW+"WARNING"+BLACK+"] " + msg.message + NL;
			print(str);
			break;
		case SEVERE:
			clear();
			str = "["+RED+"SEVERE"+BLACK+"] " + msg.message + NL;
			print(str);
			break;
		default:
			throw new RuntimeException();
		}
	}
	
	public void clear() {
		while(tempPrintLines > 0) {
			System.out.print(ERASE_LINE);
			tempPrintLines--;
		}
	}
}
