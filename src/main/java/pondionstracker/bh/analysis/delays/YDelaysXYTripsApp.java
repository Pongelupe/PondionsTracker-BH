package pondionstracker.bh.analysis.delays;


import java.io.File;
import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.function.IntFunction;
import java.util.function.ToLongFunction;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import pondionstracker.base.model.BusStatusAtStop;
import pondionstracker.bh.dto.BusStopInfo;
import pondionstracker.bh.dto.ScheduledInfo;
import pondionstracker.bh.dto.TripInfo;
import pondionstracker.utils.DateUtils;

@Slf4j
public class YDelaysXYTripsApp {


	@SneakyThrows
	public static void main(String[] args) {
		var mapper = new ObjectMapper();
		mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

		Map<Date, List<ScheduledInfo>> routesPerWeekdays = new HashMap<>();

		var targetDates = List.of(
				// Sunday
//				"30-07-2023", "06-08-2023",
				
				// Saturday
//				"29-07-2023","05-08-2023"
				
				// Weekdays
				"02-08-2023"
				, "03-08-2023"
				, "31-07-2023", "01-08-2023"
				,"04-08-2023", 
				"07-08-2023", "08-08-2023"
		);

		for (String ta : targetDates) {
			routesPerWeekdays.putAll(mapper.readValue(new File("./data/X_G_" + ta + ".json"),
					new TypeReference<Map<Date, List<ScheduledInfo>>>() {
					}));
		}

		var routesPerDayOfTheWeek = routesPerWeekdays.entrySet().stream()
				.collect(Collectors.groupingBy(e -> DateUtils.getDayOfWeekFromDate(e.getKey()))).entrySet().stream()
				.collect(Collectors.toMap(Entry<DayOfWeek, List<Entry<Date, List<ScheduledInfo>>>>::getKey,
						e -> e.getValue().stream().flatMap(r -> r.getValue().stream()).toList()));

		var trips = Set.of(DayOfWeek.values())
				.stream()
				.map(routesPerDayOfTheWeek::get)
				.filter(Objects::nonNull)
				.flatMap(List<ScheduledInfo>::stream)
				.map(ScheduledInfo::getTrips).flatMap(List<TripInfo>::stream).toList();

		var delayedBusStopInfoPerStop = trips.stream().map(TripInfo::getBusStops).flatMap(List<BusStopInfo>::stream)
				.filter(e -> e.getStatus().equals(BusStatusAtStop.DELAYED))
				.collect(Collectors.groupingBy(BusStopInfo::getIdStop));

		ToLongFunction<TripInfo> delayCount = t -> t.getBusStops().stream().map(e -> e.getStatus())
				.filter(e -> e.equals(BusStatusAtStop.DELAYED)).count();

		var totalDelay = delayedBusStopInfoPerStop.values().stream().mapToInt(List<BusStopInfo>::size)
				.sum();

		var yTrips = new ArrayList<Double>();
		var mostDelayedTrips = trips.stream()
				.sorted((o1, o2) -> Long.compare(delayCount.applyAsLong(o2), delayCount.applyAsLong(o1))).toList();
		
		var tripsSoFar = 0;
		
		for (int i = 0; i < mostDelayedTrips.size(); i++) {
			tripsSoFar += mostDelayedTrips.get(i)
					.getBusStops().stream().filter(r -> r.getStatus().equals(BusStatusAtStop.DELAYED))
					.count();
			
			log.info(i + ": {} => {}", (double) tripsSoFar / mostDelayedTrips.size(),
					((double) tripsSoFar / totalDelay) * 100);
			
			yTrips.add(((double) tripsSoFar / totalDelay) * 100);
		}

		var yBusStop = new ArrayList<Double>();
		var stops = delayedBusStopInfoPerStop.entrySet().stream()
				.sorted((o1, o2) -> Integer.compare(o2.getValue().size(), o1.getValue().size())).toList();

		IntFunction<List<Entry<String, List<BusStopInfo>>>> getBusStopsSoFar = i -> stops.subList(0, i);

		for (int i = 0; i < stops.size(); i++) {
			var busStopsSoFar = getBusStopsSoFar.apply(i);
			int totalDelayForBusStopSoFar = busStopsSoFar.stream().mapToInt(e -> e.getValue().size()).sum();

			yBusStop.add(((double) totalDelayForBusStopSoFar / totalDelay) * 100);
			log.info(i + ": {}", ((double) totalDelayForBusStopSoFar / totalDelay) * 100);

		}

		mapper.writeValue(new File("./data/ydelays_ytrips_WEEKDAYS.json"), List.of(yBusStop, yTrips));

	}

}
