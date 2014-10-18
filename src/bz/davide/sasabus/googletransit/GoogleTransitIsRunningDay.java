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

import it.bz.tis.sasabus.backend.shared.IsRunningAtDay;
import java.util.TimeZone;

public class GoogleTransitIsRunningDay implements IsRunningAtDay
{
   String            service_id;
   Calendars         calendars;

   transient boolean cacheResponse = false;
   transient String  cacheDay      = null;

   GoogleTransitIsRunningDay()
   {
   }

   public GoogleTransitIsRunningDay(String service_id, Calendars calendars)
   {
      this.service_id = service_id;
      this.calendars = calendars;
   }

   @Override
   public boolean isRunning(String yyyymmdd)
   {

      if (this.cacheDay == null || !this.cacheDay.equals(yyyymmdd))
      {
         java.util.Calendar cal = java.util.Calendar.getInstance(TimeZone.getTimeZone("Italy/Rome"));
         String yyyy = yyyymmdd.substring(0, 4);
         String mm = yyyymmdd.substring(4, 6);
         String dd = yyyymmdd.substring(6, 8);

         cal.setTimeInMillis(0);
         cal.set(java.util.Calendar.YEAR, Integer.parseInt(yyyy));
         cal.set(java.util.Calendar.MONTH, Integer.parseInt(mm) - 1);
         cal.set(java.util.Calendar.DAY_OF_MONTH, Integer.parseInt(dd));

         cal.set(java.util.Calendar.HOUR_OF_DAY, 8);

         // System.out.println(cal.getTime());

         int dayOfWeek = cal.get(java.util.Calendar.DAY_OF_WEEK);

         // Search for standard days
         int flag = -1;
         for (Calendar calendar : this.calendars.calendar)
         {
            if (calendar.service_id.equals(this.service_id))
            {
               int cmp1 = calendar.start_date.compareTo(yyyymmdd);
               int cmp2 = yyyymmdd.compareTo(calendar.end_date);
               if (cmp1 <= 0 && cmp2 <= 0) // yyyymmdd between start_date and end_date inclusive
               {
                  if (flag != -1)
                  {
                     throw new IllegalStateException("Same date range more time?");
                  }
                  switch (dayOfWeek)
                  {
                     case java.util.Calendar.MONDAY:
                        flag = calendar.monday;
                     break;
                     case java.util.Calendar.TUESDAY:
                        flag = calendar.tuesday;
                     break;
                     case java.util.Calendar.WEDNESDAY:
                        flag = calendar.wednesday;
                     break;
                     case java.util.Calendar.THURSDAY:
                        flag = calendar.thursday;
                     break;
                     case java.util.Calendar.FRIDAY:
                        flag = calendar.friday;
                     break;
                     case java.util.Calendar.SATURDAY:
                        flag = calendar.saturday;
                     break;
                     case java.util.Calendar.SUNDAY:
                        flag = calendar.sunday;
                     break;
                  }

               }
            }
         }
         // There is an exception?
         int exc = -1;
         for (CalendarDate calendarDate : this.calendars.calendarDate)
         {
            if (calendarDate.service_id.equals(this.service_id) && calendarDate.date.equals(yyyymmdd))
            {
               if (exc != -1)
               {
                  throw new IllegalStateException("exception 2 times?");
               }
               exc = calendarDate.exception_type;
            }
         }
         switch (exc)
         {
            case 1:
               flag = 1;
            break;

            case 2:
               flag = 0;
            break;
         }
         this.cacheResponse = flag == 1;
         this.cacheDay = yyyymmdd;
      }
      return this.cacheResponse;
   }
}
