/*
 * Copyright (C) 2003-2007 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */

package org.exoplatform.applications.ooplugin.client;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;

import org.exoplatform.applications.ooplugin.utils.TextUtils;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.rest.ExtHttpHeaders;

/**
 * Created by The eXo Platform SAS.
 * 
 * @version $Id: $
 */

public class HttpClient
{

   private static Log LOG = ExoLogger.getLogger(HttpClient.class);

   public static final String CLIENT_DESCRIPTION = "Exo-Http Client v.1.0.beta";

   public static final String HEADER_SPLITTER = ": ";

   public static final String CLIENT_VERSION = "HTTP/1.1";

   private String server = "";

   private int port = 0;

   private Socket clientSocket = null;

   private PrintStream outPrintStream = null;

   private OutputStream outStream = null;

   private InputStream inputStream = null;

   // Request
   private String httpCommand = "GET";

   private String httpRequestStr = "";

   private ArrayList<String> requestHeaders = new ArrayList<String>();

   private String httpRequestBodyStr;

   private byte[] httpRequestBodyBytes;

   private InputStream httpRequestBodyStream;

   // Response
   private String mainHeader = "";

   private ArrayList<String> responseHeaders = new ArrayList<String>();

   private byte[] contentBytes = null;

   public HttpClient(String server, int port)
   {
      this.server = server;
      this.port = port;
   }

   public void conect() throws Exception
   {
      for (int i = 0; i < 10; i++)
      {
         try
         {
            clientSocket = new Socket(server, port);
            outStream = clientSocket.getOutputStream();
            outPrintStream = new PrintStream(clientSocket.getOutputStream());
            inputStream = clientSocket.getInputStream();
            return;
         }
         catch (SocketException exc)
         {
            Thread.sleep(500);
         }
      }
   }

   public void setHttpCommand(String httpCommand)
   {
      this.httpCommand = httpCommand;
   }

   public void setRequestPath(String httpRequestStr)
   {
      this.httpRequestStr = httpRequestStr;
   }

   public void setRequestHeader(String headerName, String headerValue)
   {
      int existedIndex = -1;
      for (int i = 0; i < requestHeaders.size(); i++)
      {
         String curHeader = (String) requestHeaders.get(i);

         String[] curHeaderValues = curHeader.split(HEADER_SPLITTER);

         if (curHeaderValues[0].toUpperCase().equals(headerName.toUpperCase()))
         {
            existedIndex = i;
            break;
         }
      }

      if (existedIndex >= 0)
      {
         requestHeaders.remove(existedIndex);
      }

      String newHeader = headerName + HEADER_SPLITTER + headerValue;
      requestHeaders.add(newHeader);
   }

   public void setRequestBody(String httpRequestBodyStr)
   {
      this.httpRequestBodyStr = httpRequestBodyStr;
   }

   public void setRequestBody(byte[] httpRequestBodyBytes)
   {
      this.httpRequestBodyBytes = httpRequestBodyBytes;
   }

   public void setRequestStream(InputStream httpRequestBodyStream)
   {
      this.httpRequestBodyStream = httpRequestBodyStream;
   }

   public void zeroRequestBody()
   {
      this.httpRequestBodyStr = null;
   }

   public void sendRequest(String request)
   {
      outPrintStream.print(request);
   }

   public String getMainHeader()
   {
      return mainHeader;
   }

   public int getContentLength()
   {
      for (int i = 0; i < responseHeaders.size(); i++)
      {
         String curHeader = (String) responseHeaders.get(i);
         if (curHeader.startsWith(ExtHttpHeaders.CONTENTLENGTH))
         {
            String[] params = curHeader.split(":");
            String lenValue = params[1];
            lenValue = lenValue.trim();
            return new Integer(lenValue);
         }
      }

      return 0;
   }

   public ArrayList<String> getResponseHeadersNames()
   {
      ArrayList<String> result = new ArrayList<String>();
      for (int i = 0; i < responseHeaders.size(); i++)
      {
         String curHeader = responseHeaders.get(i);
         result.add(curHeader.split(":")[0]);
      }
      return result;
   }

   public String getResponseHeader(String headerName)
   {
      for (int i = 0; i < responseHeaders.size(); i++)
      {
         String curHeader = responseHeaders.get(i);
         String[] splitted = curHeader.split(": ");
         if (splitted[0].equalsIgnoreCase(headerName))
         {
            return splitted[1];
         }
      }
      return null;
   }

   public int getReplyCode()
   {
      int replyCode = 0;
      String[] mPathes = mainHeader.split(" ");
      replyCode = new Integer(mPathes[1]);
      return replyCode;
   }

   public String getResponseBody()
   {
      String contentString = "";
      for (int i = 0; i < contentBytes.length; i++)
      {
         contentString += (char) contentBytes[i];
      }
      return contentString;
   }

   public byte[] getResponseBytes()
   {
      return contentBytes;
   }

   public InputStream getResponseStream()
   {
      return new ByteArrayInputStream(contentBytes);
   }

   public int execute() throws IOException
   {
      String escapedHttpPath = TextUtils.Escape(httpRequestStr, '%', true);
      String httpLine = httpCommand + " " + escapedHttpPath + " " + CLIENT_VERSION;
      outPrintStream.println(httpLine);

      long reqContLength = 0;

      if (httpRequestBodyStream == null)
      {
         if (httpRequestBodyStr != null)
         {
            reqContLength = httpRequestBodyStr.length();
         }
         else if (httpRequestBodyBytes != null)
         {
            reqContLength = httpRequestBodyBytes.length;
         }

         setRequestHeader(ExtHttpHeaders.CONTENTLENGTH, "" + reqContLength);
      }

      setRequestHeader(ExtHttpHeaders.HOST, server + ((port == 80) ? "" : ":" + port));

      setRequestHeader(ExtHttpHeaders.USERAGENT, CLIENT_DESCRIPTION);

      for (int i = 0; i < requestHeaders.size(); i++)
      {
         String curHeader = requestHeaders.get(i);
         outPrintStream.println(curHeader);
      }

      outPrintStream.println();

      if (httpRequestBodyStream != null)
      {
         byte[] buff = new byte[4096];
         long readData = 0;
         while (true)
         {
            int readed = httpRequestBodyStream.read(buff);
            readData += readed;
            if (readed < 0)
            {
               break;
            }
            outStream.write(buff, 0, readed);
         }
      }
      else
      {
         if (reqContLength != 0)
         {
            if (httpRequestBodyStr != null)
            {
               outPrintStream.print(httpRequestBodyStr);
            }
            else
            {
               outStream.write(httpRequestBodyBytes);
            }
         }
      }

      // RESPONSE

      mainHeader = readLine();

      while (true)
      {
         String nextHeader = readLine();
         if (nextHeader.equals(""))
         {
            break;
         }

         responseHeaders.add(nextHeader);
      }

      ByteArrayOutputStream outStream = new ByteArrayOutputStream();

      // check if TransferEncoding header is set as chunked.

      String transferEncoding = getResponseHeader(ExtHttpHeaders.TRANSFER_ENCODING);

      if ("chunked".equals(transferEncoding))
      {
         while (true)
         {
            String nextLengthValue = readLine();

            int needsToRead = Integer.parseInt(nextLengthValue, 16);
            if (needsToRead == 0)
            {
               break;
            }

            byte[] buffer = new byte[needsToRead];

            while (true)
            {
               int readed = inputStream.read(buffer);
               outStream.write(buffer, 0, readed);
               if (readed == needsToRead)
               {
                  break;
               }
               needsToRead -= readed;
               buffer = new byte[needsToRead];
            }
            readLine();
         }
      }
      else
      {
         try
         {
            int contentLength = getContentLength();

            if (contentLength != 0 && !"HEAD".equals(httpCommand))
            {
               byte[] buffer = new byte[16 * 1024];
               int received = 0;

               while (received < contentLength)
               {
                  int needToRead = buffer.length;
                  if (needToRead > (contentLength - received))
                  {
                     needToRead = contentLength - received;
                  }

                  int readed = inputStream.read(buffer, 0, needToRead);

                  if (readed < 0)
                  {
                     break;
                  }

                  if (readed == 0)
                  {
                     Thread.sleep(100);
                  }

                  outStream.write(buffer, 0, readed);
                  received += readed;
               }
            }
         }
         catch (Exception exc)
         {
            LOG.info("Unhandled exception. " + exc.getMessage(), exc);
         }

      }

      contentBytes = outStream.toByteArray();

      try
      {
         clientSocket.close();
      }
      catch (Exception exc)
      {
      }

      return getReplyCode();
   }

   protected String readLine() throws IOException
   {
      byte[] buffer = new byte[4 * 1024];
      int bufPos = 0;
      byte prevByte = 0;

      while (true)
      {
         int received = inputStream.read();
         if (received < 0)
         {
            throw new RuntimeException();
         }

         buffer[bufPos] = (byte) received;
         bufPos++;

         if (prevByte == '\r' && received == '\n')
         {
            String resultLine = "";
            for (int i = 0; i < bufPos - 2; i++)
            {
               resultLine += (char) buffer[i];
            }
            return resultLine;
         }

         prevByte = (byte) received;
      }
   }

}
