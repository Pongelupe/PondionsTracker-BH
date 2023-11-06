package pondionstracker.bh.analysis;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import pondionstracker.base.model.RealTimeBusEntry;
import pondionstracker.bh.data.providers.BHRealTimeService;
import pondionstracker.bh.dto.ScheduledInfo;
import pondionstracker.bh.dto.TripInfo;
import pondionstracker.data.components.QueryExecutor;
import pondionstracker.data.configuration.PostgisConfig;
import pondionstracker.data.providers.impl.DefaultGTFSService;
import pondionstracker.integration.drivers.IntegrationDriver;

@Slf4j
public class GeneratorApp {

	@SneakyThrows
	public static void main(String... args) {
		var config = new PostgisConfig("jdbc:postgresql://localhost:5432/pondionstracker", "pondionstracker", "pondionstracker");
		var queryExecutor = new QueryExecutor(config.getConn());
		var gtfsService = new DefaultGTFSService(queryExecutor);
		var bhService = new BHRealTimeService(queryExecutor);
		
		var mapper = new ObjectMapper();
		
		var targetDates = List.of(
				"29-07-2023", 
				"30-07-2023",
				"31-07-2023", "01-08-2023", "02-08-2023", "03-08-2023", "04-08-2023", 
				"05-08-2023", 
				"06-08-2023", 
				"07-08-2023", "08-08-2023"
				);
		
		for (String ta : targetDates) {
			var targetDate = new SimpleDateFormat("dd-MM-yyyy").parse(ta);
			var routes = queryExecutor.queryAll("select route_short_name from routes", rs -> rs.getString(1), Map.of());
			
			var info = routes
				.stream()
				.map(routeShortName -> {
					var driver = IntegrationDriver.builder()
							.gtfsService(gtfsService)
							.realTimeService(bhService)
						.build();
					
					log.info(routeShortName);
					
					var route =  driver.integrate(routeShortName, targetDate);
					var totalEntries = route.getTrips()
							.stream()
							.filter(t -> t.getRealTimeTrip() != null)
							.map(t -> t.getRealTimeTrip().getEntries())
							.flatMap(List<RealTimeBusEntry>::stream)
							.count();
					
					var d = route.getRawTrips().stream().map(e -> e.getIdVehicle()).distinct().count();
					
					System.out.println(d);
					
					return ScheduledInfo.builder()
							.routeShortName(routeShortName)
							.totalEntries(totalEntries)
							.collectedTrips(route.getRawTrips().size())
							.totalTrips(route.getTrips().size())
							.matchedTrips(route.getTrips().stream().filter(t -> t.getRealTimeTrip() != null).count())
							.trips(route.getTrips().stream().filter(t -> t.getRealTimeTrip() != null).map(TripInfo::new).toList())
							.build();
					})
				.toList();
			
			mapper.writeValue(new File("./data/X_G_" + ta + ".json"), Map.of(targetDate, info));
		}
		
		
		config.close();
	}
	
}
