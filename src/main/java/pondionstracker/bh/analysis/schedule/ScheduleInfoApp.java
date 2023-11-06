package pondionstracker.bh.analysis.schedule;

import java.io.File;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import pondionstracker.bh.dto.ScheduledInfo;

@Slf4j
public class ScheduleInfoApp {

	@SneakyThrows
	public static void main(String[] args) {
		var mapper = new ObjectMapper();
		mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
		
		Map<Date, List<ScheduledInfo>> routesPerWeekdays = new HashMap<>();
		
		var targetDates = List.of("29-07-2023", "30-07-2023", "31-07-2023", "01-08-2023", "02-08-2023", 
				"03-08-2023", "04-08-2023", "05-08-2023", 
				"06-08-2023", "07-08-2023", "08-08-2023");
	
		for (String ta : targetDates) {
			routesPerWeekdays.putAll(mapper.readValue(new File("./data/X_G_" + ta + ".json"), 
					new TypeReference<Map<Date, List<ScheduledInfo>>>() {
			}));
		}
		
		for (Entry<Date, List<ScheduledInfo>> e : routesPerWeekdays.entrySet().stream().sorted((e1, e2) -> e1.getKey().compareTo(e2.getKey())).toList()) {
			var routes = e.getValue();
			log.info("date: {}", e.getKey());
			log.info("scheduled routes: {}", routes.size());
			long totalTrips = routes.stream().mapToLong(ScheduledInfo::getTotalTrips).sum();
			long collectedTrips = routes.stream().mapToLong(ScheduledInfo::getCollectedTrips).sum();
			long matchedTrips = routes.stream().mapToLong(ScheduledInfo::getMatchedTrips).sum();
			log.info("scheduled trips: {}", totalTrips);
			log.info("collected trips: {}", collectedTrips);
			log.info("matched trips: {}", matchedTrips);
			log.info("trip matched percentage: {}", (double) matchedTrips / totalTrips);
			log.info("trip matched percentage: {}", (double) matchedTrips / collectedTrips);
			log.info("********************\n");
		}

	}

}
