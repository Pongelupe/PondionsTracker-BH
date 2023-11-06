package pondionstracker.bh.analysis.schedule;

import java.io.File;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import pondionstracker.bh.dto.ScheduledInfo;

@Slf4j
public class DiscrepancyApp {
	
	@SneakyThrows
	public static void main(String[] args) {
		var mapper = new ObjectMapper();
		mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

		Map<Date, List<ScheduledInfo>> routesPerWeekdays = new HashMap<>();

		var targetDates = List.of(
				"29-07-2023", 
				"30-07-2023",
				"31-07-2023", "01-08-2023", "02-08-2023", "03-08-2023", "04-08-2023", 
				"05-08-2023", 
				"06-08-2023", 
				"07-08-2023", "08-08-2023"
				);

		for (String ta : targetDates) {
			routesPerWeekdays.putAll(mapper.readValue(new File("./data/X_G_" + ta + ".json"),
					new TypeReference<Map<Date, List<ScheduledInfo>>>() {
					}));
		}
		
		var emptyScheduledTripsByRouteShortName = routesPerWeekdays
			.entrySet()
			.stream()
			.map(Entry<Date, List<ScheduledInfo>>::getValue)
			.flatMap(e -> e.stream())
			.collect(Collectors.groupingBy(ScheduledInfo::getRouteShortName))
			.values()
			.stream()
			.map(e -> e.stream().filter(r -> r.getTotalTrips() == 0 && r.getCollectedTrips() > 0).toList())
			.flatMap(List<ScheduledInfo>::stream)
			.collect(Collectors.groupingBy(e -> e.getRouteShortName()))
			;
		
		log.info("{}", emptyScheduledTripsByRouteShortName.values().stream()
				.flatMap(e -> e.stream())
				.mapToLong(e -> e.getCollectedTrips()).sum());
		emptyScheduledTripsByRouteShortName.forEach((k, v) ->
			log.info("{} => {}", k, v.stream().mapToLong(e -> e.getCollectedTrips()).sum()));
		
		log.info("***********************");
		
		var emptyCollectedTripsByRouteShortName = routesPerWeekdays
				.entrySet()
				.stream()
				.map(Entry<Date, List<ScheduledInfo>>::getValue)
				.flatMap(e -> e.stream())
				.collect(Collectors.groupingBy(ScheduledInfo::getRouteShortName))
				.values()
				.stream()
				.map(e -> e.stream().filter(r -> r.getTotalTrips() > 0 && r.getCollectedTrips() == 0).toList())
				.flatMap(List<ScheduledInfo>::stream)
				.collect(Collectors.groupingBy(e -> e.getRouteShortName()))
				;
		
		log.info("{}", emptyCollectedTripsByRouteShortName.values().stream()
				.flatMap(e -> e.stream())
				.mapToLong(e -> e.getTotalTrips()).sum());
		emptyCollectedTripsByRouteShortName.forEach((k, v) ->
			log.info("{} => {}", k, v.stream().mapToLong(e -> e.getTotalTrips()).sum()));
		
		log.info("***********************");
		
		var emptyMatchedTripsByRouteShortName = routesPerWeekdays
				.entrySet()
				.stream()
				.map(Entry<Date, List<ScheduledInfo>>::getValue)
				.flatMap(e -> e.stream())
				.collect(Collectors.groupingBy(ScheduledInfo::getRouteShortName))
				.values()
				.stream()
				.map(e -> e.stream().filter(r -> r.getMatchedTrips() == 0 && r.getTotalTrips() > 0).toList())
				.flatMap(List<ScheduledInfo>::stream)
				.collect(Collectors.groupingBy(e -> e.getRouteShortName()))
				;
		
		log.info("{}", emptyMatchedTripsByRouteShortName.values().stream()
				.flatMap(e -> e.stream())
				.mapToLong(e -> e.getTotalTrips()).sum());
		emptyMatchedTripsByRouteShortName.forEach((k, v) ->
		log.info("{} => {}", k, v.stream().mapToLong(e -> e.getTotalTrips()).sum()));

	}
}
