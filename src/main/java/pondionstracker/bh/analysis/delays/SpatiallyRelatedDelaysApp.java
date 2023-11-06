package pondionstracker.bh.analysis.delays;

import java.awt.Color;
import java.io.File;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;

import org.knowm.xchart.BitmapEncoder;
import org.knowm.xchart.BitmapEncoder.BitmapFormat;
import org.knowm.xchart.SwingWrapper;
import org.knowm.xchart.XYChart;
import org.knowm.xchart.XYChartBuilder;
import org.knowm.xchart.style.Styler;
import org.knowm.xchart.style.markers.SeriesMarkers;

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
public class SpatiallyRelatedDelaysApp {


	@SneakyThrows
	public static void main(String[] args) {
		var mapper = new ObjectMapper();
		mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
		
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
			routesPerWeekdays.putAll(mapper.readValue(new File("./data/X_G_" + ta + ".json"), 
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
			
			
			var mostDelayedStops = Set.of("14790997", "14793268");
//			var mostDelayedStops = Set.of("14788408");
			
			var geralDelayAVG = trips
				.stream()
				.map(e -> e.getBusStops())
				.flatMap(e -> e.stream())
				.filter(e -> e.getDiffInMinutes() != null && e.getStatus().equals(BusStatusAtStop.DELAYED))
				.mapToDouble(e -> e.getDiffInMinutes())
				.average()
				.orElseThrow()
				;
			
			var geralAheadAVG = trips
					.stream()
					.map(e -> e.getBusStops())
					.flatMap(e -> e.stream())
					.filter(e -> e.getDiffInMinutes() != null && e.getStatus().equals(BusStatusAtStop.AHEAD_OF_SCHEDULE))
					.mapToDouble(e -> e.getDiffInMinutes())
					.average()
					.orElseThrow()
					;
			
			var charts = trips
				.stream()
				.map(e -> e.getBusStops())
				.flatMap(e -> e.stream())
				.filter(e -> mostDelayedStops.contains(e.getIdStop()))
				.collect(Collectors.groupingBy(e -> e.getIdStop()))
				.entrySet()
				.stream()
				.skip(0)
				.map(entry -> {
					var idStop = entry.getKey();
					var values = entry.getValue();
					
					BiFunction<LocalTime, LocalTime, Double> get = (bottom, upper) 
							-> values.stream()
							.filter(e -> DateUtils.date2localtime(e.getRealTime()).isAfter(bottom) 
									&& DateUtils.date2localtime(e.getRealTime()).isBefore(upper))
							.mapToLong(BusStopInfo::getDiffInMinutes)
							.summaryStatistics()
							.getAverage();
					
							var ys = new HashMap<Date, Double>();
							
							LocalTime bottom = null;
							
							while (!LocalTime.MIN.equals(bottom)) {
								bottom = bottom != null ? bottom : LocalTime.MIN;
								
								var upper = bottom.plusMinutes(8);
								double c = get.apply(bottom, upper);
								ys.put(DateUtils.dateFromLocalTime(new Date(), bottom), c);
								
								log.info("{} - {}", bottom, upper);
								
								bottom = bottom.plusMinutes(8);
							}
							
							Supplier<DoubleStream> ysDouble = () -> ys.values().stream().mapToDouble(e -> e);
							var chart = new XYChartBuilder()
									.title("Stop %s".formatted(idStop))
									.xAxisTitle("Time")
									.yAxisTitle("Minutes out-of-schedule")
									.width(1500)
									.height(650)
									.build();
							chart.getStyler().setYAxisMin(-30d);
						    chart.getStyler().setYAxisMax(15d);
						    chart.getStyler().setDatePattern("dd HH:ss");	
						    chart.getStyler().setLocale(Locale.GERMAN);
						    chart.getStyler().setLegendPosition(Styler.LegendPosition.InsideNE);
						    chart.getStyler().setAxisTitleFont(chart.getStyler().getAxisTitleFont().deriveFont(18.5f));
						    chart.getStyler().setChartTitleFont(chart.getStyler().getChartTitleFont().deriveFont(18.5f));
						    chart.getStyler().setLegendFont(chart.getStyler().getLegendFont().deriveFont(16.5f));
						    
						    
						    var avg  = ysDouble.get().summaryStatistics().getAverage();
						    var c = ys.keySet().stream().map(e -> geralAheadAVG).toList();
						    var c2 = ys.keySet().stream().map(e -> geralDelayAVG).toList();
						    var c1 = ys.keySet().stream().map(e -> avg).toList();
						    var c0 = ys.keySet().stream().map(e -> 0).toList();
						    
						    log.info("geralAheadAVG : {}", geralAheadAVG);
						    log.info("geralDelayAVG : {}", geralDelayAVG);
						    log.info("avg : {}", avg);
						    
						    chart.addSeries("Buses at the ", ys.keySet().stream().sorted().toList(), 
						    		ys.values().stream().toList())
						    .setLineColor(Color.blue)
						    .setFillColor(Color.blue)
						    	.setShowInLegend(false)
						    ;
						    chart.addSeries("Global Ahead Average", ys.keySet().stream().sorted().toList(), c)
						    	.setMarker(SeriesMarkers.NONE)
						    	.setLineColor(Color.green);
						    chart.addSeries("Global Delay Average", ys.keySet().stream().sorted().toList(), c2)
						    .setMarker(SeriesMarkers.NONE)
						    .setLineColor(Color.red);
						    chart.addSeries("Local Out-Of-Schedule Average", ys.keySet().stream().sorted().toList(), c1)
						    .setMarker(SeriesMarkers.NONE)
						    .setLineColor(Color.orange);
						    chart.addSeries("0", ys.keySet().stream().sorted().toList(), c0)
						    .setMarker(SeriesMarkers.NONE)
						    .setLineColor(Color.black)
						    .setShowInLegend(false);
						    return chart;
				})
				.toList();
			
			BitmapEncoder.saveBitmap(charts, mostDelayedStops.size(), 1,
					"./charts/stops%d".formatted(mostDelayedStops.size()), BitmapFormat.PNG);
			new SwingWrapper<XYChart>(charts).displayChartMatrix();
			
	}

}
