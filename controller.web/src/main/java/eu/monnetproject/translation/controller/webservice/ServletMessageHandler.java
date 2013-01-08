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
package eu.monnetproject.translation.controller.webservice;

import eu.monnetproject.translation.monitor.*;
import static eu.monnetproject.translation.monitor.Messages.MessageType.*;
import javax.servlet.ServletContext;

/**
 * @author John McCrae
 */
 public class ServletMessageHandler implements MessageHandler {
 	 private final ServletContext context;
 	 
 	 public ServletMessageHandler(ServletContext context) {
 	 	 this.context = context;
 	 }
 	 
 	 
 	 @Override public void handle(Messages.Message msg) {
 	 	 switch(msg.type) {
 	 	 case WARNING:
 	 	 case SEVERE:
 	 	 	 context.log("["+msg.job.id()+"] "+msg.message);
 	 	 	 break;
 	 	 case FAIL:
 	 	 case CFAIL:
 	 	 case CLEAN_FAIL:
 	 	 	 if(msg.cause != null) {
 	 	 	 	 context.log("["+msg.job.id()+"] "+msg.message,msg.cause);
 	 	 	 } else {
 	 	 	 	 context.log("["+msg.job.id()+"] "+msg.message);
 	 	 	 }
 	 	 	 break;
 	 	 default:
 	 	 }
 	 }
 	 	 	 
	
 	 @Override public void beginJob(Job job) {
 	 	 context.log("["+job.id()+"] Started");
 	 }
	
 	 @Override public void endJob(Job job) {
 	 	 context.log("["+job.id()+"] Finished");
 	 }
 	 
 }
