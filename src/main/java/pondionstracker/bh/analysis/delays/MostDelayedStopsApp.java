package pondionstracker.bh.analysis.delays;

import java.io.File;
import java.nio.file.Paths;
import java.time.DayOfWeek;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.IntFunction;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.jenetics.jpx.GPX;
import io.jenetics.jpx.WayPoint;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import pondionstracker.base.model.BusStatusAtStop;
import pondionstracker.bh.dto.BusStopInfo;
import pondionstracker.bh.dto.ScheduledInfo;
import pondionstracker.bh.dto.TripInfo;
import pondionstracker.data.components.QueryExecutor;
import pondionstracker.data.configuration.PostgisConfig;
import pondionstracker.data.constants.Query.Parameter;
import pondionstracker.utils.DateUtils;

@Slf4j
public class MostDelayedStopsApp {
	
	private static final String QUERY_STOPS = """ 
			SELECT ST_Y(stop_loc::geometry), ST_X(stop_loc::geometry) from stops
			where stop_id in (:LINE_ID)
			""";


	@SneakyThrows
	public static void main(String[] args) {
		var mapper = new ObjectMapper();
		mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
		
		var config = new PostgisConfig("jdbc:postgresql://localhost:5432/pondionstracker", "pondionstracker", "pondionstracker");
		var queryExecutor = new QueryExecutor(config.getConn());
		
		Map<Date, List<ScheduledInfo>> routesPerWeekdays = new HashMap<>();
		
		var targetDates = List.of(
//				"29-07-2023","02-08-2023"
//				,"30-07-2023", "03-08-2023"
				 "31-07-2023", "01-08-2023", "02-08-2023" , "03-08-2023", "04-08-2023",
//				,"04-08-2023", "05-08-2023", 
//				"06-08-2023" 
				"07-08-2023",
				"08-08-2023"
				);
	
		for (String ta : targetDates) {
			routesPerWeekdays.putAll(mapper.readValue(new File("./X_G_" + ta + ".json"), 
					new TypeReference<Map<Date, List<ScheduledInfo>>>() {
			}));
		}
		
		var routesPerDayOfTheWeek = routesPerWeekdays
				.entrySet()
				.stream()
				.collect(Collectors.groupingBy(e -> DateUtils.getDayOfWeekFromDate(e.getKey())))
				.entrySet()
				.stream()
				.collect(Collectors.toMap(Entry<DayOfWeek, List<Entry<Date, List<ScheduledInfo>>>>::getKey, 
						e -> e.getValue().stream().flatMap(r -> r.getValue().stream()).toList()))
				.values()
				.stream()
				.flatMap(List<ScheduledInfo>::stream)
				.toList();
		
		
		var trips = routesPerDayOfTheWeek
			.stream()
			.map(ScheduledInfo::getTrips)
			.flatMap(List<TripInfo>::stream)
			.toList();
		
		IntFunction<List<Entry<String, List<BusStopInfo>>>> getmostDelayedStops = i -> trips
				.stream()
				.map(TripInfo::getBusStops)
				.flatMap(List<BusStopInfo>::stream)
				.filter(e -> e.getStatus().equals(BusStatusAtStop.DELAYED))
				.collect(Collectors.groupingBy(BusStopInfo::getIdStop))
				.entrySet()
				.stream()
				.sorted((o1, o2) -> Integer.compare(o2.getValue().size(), o1.getValue().size()))
				.limit(i)
				.toList()
			;
		
		var i = 5;
		
		var mostDelayedStops = getmostDelayedStops.apply(i);
		
		log.info("most delayed stops ({}) {}", i, mostDelayedStops);
		
		mostDelayedStops.forEach(e -> {
			log.info(e.getKey() + ": {} ", e.getValue().size()
					);
		});
		
		var builder = GPX.builder();
		queryExecutor.queryAll(QUERY_STOPS, rs -> WayPoint.builder()
				.lat(rs.getDouble(1))
				.lon(rs.getDouble(2))
				.build(), Map.of(Parameter.LINE_ID, mostDelayedStops))
		.forEach(builder::addWayPoint);
		
		GPX gpx = builder.build();
		GPX.write(gpx, Paths.get("./data/delay_stop/mostDelayedStops_%s.gpx".formatted(i)));
		
		config.close();
					
	}

}
