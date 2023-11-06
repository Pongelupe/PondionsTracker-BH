package pondionstracker.bh.analysis.delays;

import static java.time.DayOfWeek.*;

import java.io.File;
import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Function;
import java.util.function.ToLongFunction;
import java.util.stream.Collectors;

import org.apache.commons.math3.util.Pair;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import pondionstracker.base.model.BusStatusAtStop;
import pondionstracker.bh.dto.ScheduledInfo;
import pondionstracker.bh.dto.TripInfo;
import pondionstracker.utils.DateUtils;

@Slf4j
public class AppDelays {
	
	private record StopInfoRecord(Long diff, BusStatusAtStop s) {}

	@SneakyThrows
	public static void main(String[] args) {
		var mapper = new ObjectMapper();
		mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
		
		Map<Date, List<ScheduledInfo>> routesPerWeekdays = new HashMap<>();
		Map<Date, List<ScheduledInfo>> routesPerWeekdaysGeneretared = new HashMap<>();
		
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
			routesPerWeekdaysGeneretared.putAll(mapper.readValue(new File("./Generated_X_G_" + ta + ".json"), 
					new TypeReference<Map<Date, List<ScheduledInfo>>>() {
			}));
		}
		
		var generatedRoutes = routesPerWeekdaysGeneretared
			.entrySet()
			.stream()
			.collect(Collectors.groupingBy(e -> DateUtils.getDayOfWeekFromDate(e.getKey())))
			.entrySet()
			.stream()
			.collect(Collectors.toMap(Entry<DayOfWeek, List<Entry<Date, List<ScheduledInfo>>>>::getKey, 
					e -> e.getValue().stream().flatMap(r -> r.getValue().stream()).toList()));
		
		Function<List<ScheduledInfo>, Map<TripInfo, List<StopInfoRecord>>> getPP = 
				routes -> routes.stream().map(ScheduledInfo::getTrips)
				.flatMap(List<TripInfo>::stream)
				.collect(Collectors.toMap(Function.identity(), 
						t -> t.getBusStops().stream().map(s -> new StopInfoRecord(s.getDiffInMinutes(), s.getStatus())).toList()));
		
		
		ToLongFunction<List<ScheduledInfo>> getCompletlyOnTimeRoutes = 
				pp -> getPP.apply(pp)
				.entrySet()
				.stream()
				.filter(w -> w.getValue().stream()
						.filter(s -> !s.s.equals(BusStatusAtStop.UNKOWN))
						.allMatch(s -> s.s.equals(BusStatusAtStop.ON_TIME)))
				.count();
			
		ToLongFunction<List<ScheduledInfo>> getCompletlyOutTimeRoutes =	
				pp -> getPP.apply(pp)
				.entrySet()
				.stream()
				.filter(w -> w.getValue().stream()
						.filter(s -> !s.s.equals(BusStatusAtStop.UNKOWN))
						.noneMatch(s -> s.s.equals(BusStatusAtStop.ON_TIME)))
				.count();
		
		ToLongFunction<List<ScheduledInfo>> getDepartureORArrivalOnTimeRoutes =	
				pp -> getPP.apply(pp)
				.entrySet()
				.stream()
				.filter(w -> w.getValue().get(0).s.equals(BusStatusAtStop.ON_TIME)
						|| w.getValue().get(w.getValue().size() -1 ).s.equals(BusStatusAtStop.ON_TIME))
				.count()
				;
		ToLongFunction<List<ScheduledInfo>> getDepartureANDArrivalOnTimeRoutes =	
				pp -> getPP.apply(pp)
				.entrySet()
				.stream()
				.filter(w -> w.getValue().get(0).s.equals(BusStatusAtStop.ON_TIME)
						&& w.getValue().get(w.getValue().size() -1 ).s.equals(BusStatusAtStop.ON_TIME))
				.count()
				;
				
		Function<List<ScheduledInfo>, Map<BusStatusAtStop, Long>> getRawStatus =
				pp -> getPP.apply(pp)
					.values()
					.stream()
					.flatMap(e -> e.stream())
					.map(e -> e.s())
					.collect(Collectors.groupingBy(e -> e, Collectors.counting()))
					;
				
		var r = routesPerWeekdays.entrySet()
			.stream()
			.collect(Collectors.groupingBy(e -> DateUtils.getDayOfWeekFromDate(e.getKey())))
			.entrySet()
			.stream()
			.collect(Collectors.toMap(Entry<DayOfWeek, List<Entry<Date, List<ScheduledInfo>>>>::getKey,
					e -> e.getValue().stream().flatMap(e1 -> e1.getValue().stream()).toList()));
		
		Function<Set<DayOfWeek>, Pair<List<ScheduledInfo>, List<ScheduledInfo>>> getPair = s -> s.stream()
				.map(dow -> {
					var e = r.get(dow);
					return Pair.create(e, generatedRoutes.get(dow));
				})
				.reduce(new Pair<>(new ArrayList<>(), new ArrayList<>()), 
						(acc, el) -> {
							acc.getFirst().addAll(el.getFirst());
							acc.getSecond().addAll(el.getSecond());
							
							return acc;
				});
		
		List.of(getPair.apply(Set.of(MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY)),
				getPair.apply(Set.of(SATURDAY)),
				getPair.apply(Set.of(SUNDAY))
				)
		.forEach(pair -> {
			var scheduledRoutes = pair.getFirst();
			var generatedRoutes1 = pair.getSecond();
			
			log.info("Scheduled Trips");
			log.info("total trips: {}", scheduledRoutes.stream().mapToLong(w -> w.getTrips().size()).sum());
			log.info("trips completely out of schedule: {}", getCompletlyOutTimeRoutes.applyAsLong(scheduledRoutes));
			log.info("trips with departure or arrival on time: {}", getDepartureORArrivalOnTimeRoutes.applyAsLong(scheduledRoutes));
			log.info("trips with departure and arrival on time: {}", getDepartureANDArrivalOnTimeRoutes.applyAsLong(scheduledRoutes));
			log.info("trips completely on time: {}", getCompletlyOnTimeRoutes.applyAsLong(scheduledRoutes));
			getRawStatus.apply(scheduledRoutes)
				.forEach((s, l) -> log.info("{}: {} times", s, l));
			log.info("******************");
			log.info("Generated Trips");
			log.info("total trips: {}", generatedRoutes1.stream().mapToLong(w -> w.getTrips().size()).sum());
			log.info("trips completely out of schedule: {}", getCompletlyOutTimeRoutes.applyAsLong(generatedRoutes1));
			log.info("trips with departure or arrival on time: {}", getDepartureORArrivalOnTimeRoutes.applyAsLong(generatedRoutes1));
			log.info("trips with departure and arrival on time: {}", getDepartureANDArrivalOnTimeRoutes.applyAsLong(generatedRoutes1));
			log.info("trips completely on time: {}", getCompletlyOnTimeRoutes.applyAsLong(generatedRoutes1));
			getRawStatus.apply(generatedRoutes1)
			.forEach((s, l) -> log.info("{}: {} times", s, l));
			log.info("******************\n\n");
		});
		
	}

}
