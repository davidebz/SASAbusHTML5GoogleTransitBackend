/*
GoogleTransitBackend - read google transing files

Copyright (C) 2014 Davide Montesin <d@vide.bz> - Bolzano/Bozen - Italy

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as
published by the Free Software Foundation, either version 3 of the
License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program. If not, see <http://www.gnu.org/licenses/>.
*/

package bz.davide.sasabus.googletransit;

import it.bz.tis.sasabus.backend.server.SASAbusDB2JSONPServlet;
import java.io.File;
import java.io.IOException;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class GoogleTransitBackendServlet extends SASAbusDB2JSONPServlet
{
   @Override
   public void init(ServletConfig config) throws ServletException
   {
      try
      {

         File dataDir = new File(config.getServletContext().getRealPath("/WEB-INF/data/"));
         //File extrauDir = new File(config.getServletContext().getRealPath("/WEB-INF/data-extrau/"));

         GoogleTransitSASAbusDB sasabusDB = GoogleTransitSASAbusDB.loadGTData(dataDir);

         this.data.setSasabusdb(sasabusDB);
         synchronized (this.data)
         {
            this.initFinished = true;
         }
      }
      catch (Exception exxx)
      {
         throw new ServletException(exxx);
      }
   }

   @Override
   protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
   {
      super.doGet(req, resp);
   }

   @Override
   protected void sendMail(String subject, String body)
   {
      System.out.println(subject);
      System.out.println(body);
   }

}
