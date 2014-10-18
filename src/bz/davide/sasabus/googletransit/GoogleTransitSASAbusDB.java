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

import it.bz.tis.sasabus.backend.server.sasabusdb.SASAbusDBServerImpl;
import it.bz.tis.sasabus.backend.shared.Area;
import it.bz.tis.sasabus.backend.shared.BusLine;
import it.bz.tis.sasabus.backend.shared.BusStation;
import it.bz.tis.sasabus.backend.shared.BusStop;
import it.bz.tis.sasabus.backend.shared.BusTrip;
import it.bz.tis.sasabus.backend.shared.BusTripStop;
import it.bz.tis.sasabus.backend.shared.BusTripStopReference;
import it.bz.tis.sasabus.backend.shared.LatLng;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;

public class GoogleTransitSASAbusDB extends SASAbusDBServerImpl
{

   public GoogleTransitSASAbusDB(Area[] areas,
                                 HashMap<String, ArrayList<BusTripStopReference>> busTripStopByBusStationId,
                                 HashMap<String, BusStation> busStationsById,
                                 ArrayList<BusTrip> trips)
   {
      this.areas = areas;
      this.busTripStopByBusStationId = busTripStopByBusStationId;
      this.lastModified = System.currentTimeMillis();
      this.busStationsById = busStationsById;
      this.trips = trips;
   }

   public static GoogleTransitSASAbusDB loadGTData(File dataDir) throws IOException
   {
      HashMap<String, BusStation> busStationsById = new HashMap<>();

      HashMap<Integer, BusStop> busStopById = new HashMap<Integer, BusStop>();
      loadBusStops(busStopById, new File(dataDir, "stops.txt"), busStationsById);

      ArrayList<LatLng> bounds = new ArrayList<LatLng>();
      loadBounds(bounds, new File(dataDir, "tn_hull.csv"));

      Area tn = new Area(1, "TN", "TN", bounds.toArray(new LatLng[0]));

      bounds = new ArrayList<LatLng>();
      loadBounds(bounds, new File(dataDir, "rovereto_hull.csv"));

      Area rovereto = new Area(2, "Rovereto", "Rovereto", bounds.toArray(new LatLng[0]));

      //Area extrau = new Area(3, "Extraurbano", "Extraurbano", new LatLng[0]);

      Area[] areas = new Area[] { tn, rovereto };

      loadBusLines(new File(dataDir, "routes.txt"), tn, rovereto);

      //this.loadBusLines(new File(extrauDir, "routes.txt"), extrau);

      // Load calendars
      Calendars calendars = loadCalendars(new File(dataDir, "calendar.txt"), new File(dataDir,
                                                                                      "calendar_dates.txt"));

      ArrayList<BusTrip> busTrips = new ArrayList<>();

      HashMap<String, Integer> tripIdString2Int = new HashMap<String, Integer>();
      HashMap<Integer, ArrayList<BusTrip>> busTripByBusLine = new HashMap<Integer, ArrayList<BusTrip>>();
      loadBusTrip(busTripByBusLine, new File(dataDir, "trips.txt"), tripIdString2Int, calendars, busTrips);

      HashMap<Integer, ArrayList<BusTripStop>> busTripStopByBusTrip = new HashMap<Integer, ArrayList<BusTripStop>>();
      loadBusTripStop(busTripStopByBusTrip, new File(dataDir, "stop_times.txt"), tripIdString2Int);

      HashMap<String, ArrayList<BusTripStopReference>> busTripStopByBusStationId = new HashMap<String, ArrayList<BusTripStopReference>>();

      for (Area area : areas)
      {
         IdentityHashMap<BusStop, Void> areaUniqueBusStops = new IdentityHashMap<BusStop, Void>();
         for (BusLine busLine : area.getBusLines())
         {
            IdentityHashMap<BusStop, Void> busLineUniqueBusStops = new IdentityHashMap<BusStop, Void>();

            for (BusTrip busTrip : busTripByBusLine.get(busLine.getId()))
            {
               ArrayList<BusTripStop> busTripStopList = new ArrayList<BusTripStop>();

               for (BusTripStop busTripStop : busTripStopByBusTrip.get(busTrip.getId()))
               {
                  BusStop busStop = busStopById.get(busTripStop.getBusStopId());
                  busLineUniqueBusStops.put(busStop, null);

                  // BusTripStopCache
                  ArrayList<BusTripStopReference> busTripStops = busTripStopByBusStationId.get(busStop.getBusStation().getId());
                  if (busTripStops == null)
                  {
                     busTripStops = new ArrayList<BusTripStopReference>();
                     busTripStopByBusStationId.put(busStop.getBusStation().getId(), busTripStops);
                  }
                  busTripStops.add(new BusTripStopReference(busTrip, busTripStopList.size()));

                  busTripStopList.add(busTripStop);
               }
               busTrip.setBusTripStop(busTripStopList.toArray(new BusTripStop[0]));
            }
            BusStop[] busLineStops = new ArrayList<BusStop>(busLineUniqueBusStops.keySet()).toArray(new BusStop[0]);
            busLine.setBusStops(busLineStops);

            areaUniqueBusStops.putAll(busLineUniqueBusStops);
         }
         System.out.println("Area ==== " + area.getName_it());
         for (BusStop busStop : areaUniqueBusStops.keySet())
         {
            System.out.println(String.format("%f,%f", busStop.getLat(), busStop.getLon()));
         }
      }

      GoogleTransitSASAbusDB sasabusDB = new GoogleTransitSASAbusDB(areas,
                                                                    busTripStopByBusStationId,
                                                                    busStationsById,
                                                                    busTrips);

      return sasabusDB;
   }

   private static Calendars loadCalendars(File calendarFile, File calendarDatesFile) throws IOException
   {
      Calendars calendars = new Calendars();

      ArrayList<String[]> csvData = loadCSV(calendarFile);
      ArrayList<Calendar> calendarArr = new ArrayList<>();
      for (int i = 1; i < csvData.size(); i++)
      {
         String[] row = csvData.get(i);
         Calendar calendar = new Calendar();
         calendar.service_id = row[0];
         calendar.monday = Integer.parseInt(row[1]);
         calendar.tuesday = Integer.parseInt(row[2]);
         calendar.wednesday = Integer.parseInt(row[3]);
         calendar.thursday = Integer.parseInt(row[4]);
         calendar.friday = Integer.parseInt(row[5]);
         calendar.saturday = Integer.parseInt(row[6]);
         calendar.sunday = Integer.parseInt(row[7]);
         calendar.start_date = row[8];
         calendar.end_date = row[9];

         calendarArr.add(calendar);
      }
      calendars.calendar = calendarArr.toArray(new Calendar[0]);

      csvData = loadCSV(calendarDatesFile);
      ArrayList<CalendarDate> calendarDateArr = new ArrayList<>();
      for (int i = 1; i < csvData.size(); i++)
      {
         String[] row = csvData.get(i);
         CalendarDate calendarDate = new CalendarDate();

         calendarDate.service_id = row[0];
         calendarDate.date = row[1];
         calendarDate.exception_type = Integer.parseInt(row[2]);

         calendarDateArr.add(calendarDate);
      }
      calendars.calendarDate = calendarDateArr.toArray(new CalendarDate[0]);
      return calendars;
   }

   private static void loadBounds(ArrayList<LatLng> bounds, File file) throws IOException
   {
      ArrayList<String[]> csvData = loadCSV(file);
      for (int i = 1; i < csvData.size(); i++)
      {
         String[] row = csvData.get(i);
         LatLng latLng = new LatLng(Double.parseDouble(row[0]), Double.parseDouble(row[1]));
         bounds.add(latLng);
      }
   }

   private static void loadBusTripStop(HashMap<Integer, ArrayList<BusTripStop>> busTripStopByBusTrip,
                                       File file,
                                       HashMap<String, Integer> tripIdString2Int) throws IOException
   {
      ArrayList<String[]> csvData = loadCSV(file);
      for (int i = 1; i < csvData.size(); i++)
      {
         String[] row = csvData.get(i);
         String[] hhmmssArr = row[2].split(":");
         int hhmmss = Integer.parseInt(hhmmssArr[0])
                      * 10000
                      + Integer.parseInt(hhmmssArr[1])
                      * 100
                      + Integer.parseInt(hhmmssArr[0]);
         BusTripStop busTripStop = new BusTripStop(i, hhmmss, Integer.parseInt(row[3]));
         int busTripId = tripIdString2Int.get(row[0]);
         ArrayList<BusTripStop> list = busTripStopByBusTrip.get(busTripId);
         if (list == null)
         {
            list = new ArrayList<BusTripStop>();
            busTripStopByBusTrip.put(busTripId, list);
         }
         list.add(busTripStop);
      }
   }

   private static void loadBusTrip(HashMap<Integer, ArrayList<BusTrip>> busTripByBusLine,
                                   File file,
                                   HashMap<String, Integer> tripIdString2Int,
                                   Calendars calendars,
                                   ArrayList<BusTrip> busTrips) throws IOException
   {
      ArrayList<String[]> csvData = loadCSV(file);
      for (int i = 1; i < csvData.size(); i++)
      {
         String[] row = csvData.get(i);
         Integer tmp = tripIdString2Int.get(row[2]);
         if (tmp == null)
         {
            tmp = tripIdString2Int.size();
            tripIdString2Int.put(row[2], tmp);
         }
         int id = tmp;
         int busLineId = Integer.parseInt(row[0]);
         BusTrip busTrip = new BusTrip(id, new GoogleTransitIsRunningDay(row[1], calendars), busLineId);
         ArrayList<BusTrip> list = busTripByBusLine.get(busLineId);
         if (list == null)
         {
            list = new ArrayList<BusTrip>();
            busTripByBusLine.put(busLineId, list);
         }
         list.add(busTrip);
         busTrips.add(busTrip);
      }
   }

   private static void loadBusLines(File file, Area tn, Area rovereto) throws IOException
   {
      ArrayList<String[]> csvData = loadCSV(file);
      for (int i = 1; i < csvData.size(); i++)
      {
         String[] row = csvData.get(i);
         int busLineId = Integer.parseInt(row[0]);
         Area area = tn;
         switch (busLineId)
         {
            case 488:
            case 490:
            case 527:
            case 529:
            case 496:
            case 512:
            case 486:
            case 506:
            case 504:
            case 507:
            case 510:
            case 511:
            case 497:
               area = rovereto;
         }
         BusLine busLine = new BusLine(busLineId, row[2] + " (" + busLineId + ")", area);
         busLine.setBusStops(new BusStop[0]);
      }

   }

   private static void loadBusLines(File file, Area extrau) throws IOException
   {
      ArrayList<String[]> csvData = loadCSV(file);
      for (int i = 1; i < csvData.size(); i++)
      {
         String[] row = csvData.get(i);
         int busLineId = Integer.parseInt(row[0]);
         BusLine busLine = new BusLine(busLineId, row[2] + " (" + busLineId + ")", extrau);
         busLine.setBusStops(new BusStop[0]);
      }

   }

   private static void loadBusStops(HashMap<Integer, BusStop> busStopById,
                                    File stops,
                                    HashMap<String, BusStation> busStationsById) throws IOException
   {
      ArrayList<String[]> csvData = loadCSV(stops);
      for (int i = 1; i < csvData.size(); i++)
      {
         String[] row = csvData.get(i);
         String name = row[1];
         BusStation busStation = busStationsById.get(name);
         if (busStation == null)
         {
            busStation = new BusStation(name, name);
            busStationsById.put(name, busStation);
         }
         int id = Integer.parseInt(row[0]);
         BusStop busStop = new BusStop(busStation, id, Double.parseDouble(row[3]), Double.parseDouble(row[4]));
         busStopById.put(id, busStop);
      }
   }

   static ArrayList<String[]> loadCSV(File file) throws IOException
   {
      ArrayList<String[]> ret = new ArrayList<String[]>();
      FileReader fileReader = new FileReader(file);
      BufferedReader bufferedReader = new BufferedReader(fileReader);
      String line;
      while ((line = bufferedReader.readLine()) != null)
      {
         line = line.trim();
         if (line.length() > 0)
         {
            String[] columns = line.split(" *, *");
            ret.add(columns);
         }
      }
      bufferedReader.close();
      fileReader.close();
      return ret;
   }
}
