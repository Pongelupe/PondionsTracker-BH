package pondionstracker.bh.analysis.delays;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import pondionstracker.base.model.BusStatusAtStop;
import pondionstracker.base.model.RealTimeBusEntry;
import pondionstracker.bh.dto.ScheduledInfo;
import pondionstracker.bh.dto.TripInfo;
import pondionstracker.data.components.QueryExecutor;
import pondionstracker.data.configuration.PostgisConfig;
import pondionstracker.data.providers.impl.DefaultGTFSService;
import pondionstracker.integration.impl.DefaultTripExpectedTimeGenerator;

@Slf4j
public class PondionsTrackerTimeTableGeneratorApp {

	@SneakyThrows
	public static void main(String[] args) {
		var config = new PostgisConfig("jdbc:postgresql://localhost:5432/pondionstracker", "pondionstracker", "pondionstracker");
		var queryExecutor = new QueryExecutor(config.getConn());
		var gtfsService = new DefaultGTFSService(queryExecutor);
		var tripExpectedTimeGenerator = new DefaultTripExpectedTimeGenerator();
		
		var mapper = new ObjectMapper();
		mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
		
		Map<Date, List<ScheduledInfo>> routesPerWeekdays = new HashMap<>();
		
		var targetDates = List.of("29-07-2023"
				, 
				"30-07-2023", "31-07-2023", "01-08-2023", "02-08-2023", 
				"03-08-2023", "04-08-2023", "05-08-2023", 
				"06-08-2023", "07-08-2023", "08-08-2023"
				);
		
		for (String ta : targetDates) {
			routesPerWeekdays.putAll(mapper.readValue(new File("./data/X_G_" + ta + ".json"), 
					new TypeReference<Map<Date, List<ScheduledInfo>>>() {
			}));
		}
	
		for (String ta : targetDates) {
			log.info(ta);
			var targetDate = new SimpleDateFormat("dd-MM-yyyy").parse(ta);
			var routes = queryExecutor.queryAll("select route_short_name from routes", rs -> rs.getString(1), Map.of());
			
			var tripInfoByTripId = routesPerWeekdays.get(targetDate)
				.stream()
				.map(ScheduledInfo::getTrips)
				.flatMap(List<TripInfo>::stream)
				.collect(Collectors.toMap(e -> e.getTripId(), Function.identity()));
			
			var info = routes
				.stream()
				.map(routeShortName -> {
					var route = gtfsService.getRouteByRouteShortName(routeShortName, targetDate)
							.orElseThrow();
					
					var tripsInfo = route
					.getTrips()
					.stream()
					.filter(trip -> tripInfoByTripId.containsKey(trip.getTripId()))
				 	.map(trip -> {
				 		var stopsIntervals = gtfsService.getStopPointsInterval(trip.getTripId());
				 		var busStopsTrip = tripExpectedTimeGenerator.generate(trip, stopsIntervals)
				 				.stream()
				 				.collect(Collectors.toMap(e -> e.getStopSequence(), Function.identity()));
				 		
				 		
				 		var tripInfo = tripInfoByTripId.get(trip.getTripId());
				 		
				 		for (int i = 0; i < tripInfo.getBusStops().size(); i++) {
							var stop = tripInfo.getBusStops().get(i);
							
							Optional.ofNullable(busStopsTrip.get(stop.getStopSequence()))
								.ifPresentOrElse(newBusStop ->  {
									newBusStop.setEntries(List.of(RealTimeBusEntry.builder().dtEntry(stop.getRealTime()).build()));
									
									stop.setExpectedTime(newBusStop.getExpectedTime());
									stop.setDiffInMinutes(newBusStop.getExpectedRealTimeDiffInMinutes());
									stop.setStatus(newBusStop.getStatus());
								}, () -> {
									stop.setExpectedTime(null);
									stop.setDiffInMinutes(null);
									stop.setStatus(BusStatusAtStop.UNKOWN);
								});
						}
				 		
				 		return tripInfo;
				 	})
				 	.toList();
					
					return ScheduledInfo.builder()
							.routeShortName(routeShortName)
							.trips(tripsInfo)
							.build();
				})
			.toList();
			
			mapper.writeValue(new File("./data/Generated_X_G_" + ta + ".json"), Map.of(targetDate, info));
			
		}
		
		config.close();
		
	}

}
