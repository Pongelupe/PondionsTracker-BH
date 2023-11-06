package pondionstracker.bh.analysis.schedule;

import static java.time.DayOfWeek.FRIDAY;
import static java.time.DayOfWeek.MONDAY;
import static java.time.DayOfWeek.SATURDAY;
import static java.time.DayOfWeek.SUNDAY;
import static java.time.DayOfWeek.THURSDAY;
import static java.time.DayOfWeek.TUESDAY;
import static java.time.DayOfWeek.WEDNESDAY;

import java.awt.Color;
import java.io.File;
import java.text.SimpleDateFormat;
import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Function;

import org.knowm.xchart.BitmapEncoder;
import org.knowm.xchart.BitmapEncoder.BitmapFormat;
import org.knowm.xchart.CategoryChart;
import org.knowm.xchart.CategoryChartBuilder;
import org.knowm.xchart.Histogram;
import org.knowm.xchart.style.Styler;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.SneakyThrows;
import pondionstracker.bh.dto.ScheduledInfo;
import pondionstracker.utils.DateUtils;

public class HistogramApp {
	
	private static final Map<DayOfWeek, Color> COLORS = Map.of(SATURDAY, Color.red, SUNDAY, Color.orange);

	@SneakyThrows
	public static void main(String[] args) {
		var mapper = new ObjectMapper();
		mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
		
		var f = new SimpleDateFormat("dd-MM-yyyy");

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
		
		var charts = new ArrayList<>(routesPerWeekdays
			.entrySet()
			.stream()
			.sorted((o1, o2) -> o1.getKey().compareTo(o2.getKey()))
			.map(entry -> {
				CategoryChart chart = new CategoryChartBuilder()
						.width(300)
						.height(200)
//						.title("Schedule-Filled Percentage per Routes Distribution")
						.title(f.format(entry.getKey()))
						.xAxisTitle("Schedule-Filled Percentage")
						.yAxisTitle("Routes")
						.build();
				chart.getStyler().setLegendPosition(Styler.LegendPosition.InsideNW);
				chart.getStyler().setLegendVisible(false);
				chart.getStyler().setAvailableSpaceFill(1);
				
				chart.getStyler().setXAxisLabelRotation(25);
				
				var data = entry.getValue()
					.stream()
					.map(e -> (double) e.getMatchedTrips() / e.getTotalTrips())
					.sorted()
					.toList();
				
				var histogram = new Histogram(data, 20);
				
				chart.addSeries("Percentage Filled Group", 
						histogram.getxAxisData(), histogram.getyAxisData())
				.setFillColor(COLORS.getOrDefault(DateUtils.getDayOfWeekFromDate(entry.getKey()), Color.blue));
				
				chart.getStyler().setxAxisTickLabelsFormattingFunction(d -> String.format("%.2f%%", d * 100));
				
				return chart;
			})
			.toList());
			
		
			Function<Set<DayOfWeek>, List<Double>> filter2 = set -> routesPerWeekdays
						.entrySet()
						.stream()
						.filter(e -> set
								.contains(DateUtils.getDayOfWeekFromDate(e.getKey())))
						.map(Entry<Date, List<ScheduledInfo>>::getValue)
						.flatMap(e -> e.stream())
						.map(e -> (double) e.getMatchedTrips() / e.getTotalTrips())
						.sorted()
						.toList()
			;
		

		CategoryChart chart = new CategoryChartBuilder()
				.width(300)
				.height(200)
				.title("Observation Period Aggregated")
				.xAxisTitle("Schedule-Filled Percentage")
				.yAxisTitle("Routes")
				.build();
		chart.getStyler().setLegendPosition(Styler.LegendPosition.InsideNW);
		chart.getStyler().setLegendVisible(false);
		chart.getStyler().setAvailableSpaceFill(1);
		 
		var s = new Histogram(filter2.apply(Set.of(MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY)), 20);
		var s1 = new Histogram(filter2.apply(Set.of(SATURDAY)), 20);
		var s2 = new Histogram(filter2.apply(Set.of(SUNDAY)), 20);
		
		System.out.println(s1.getxAxisData());
		System.out.println(s1.getyAxisData());

		System.out.println(s2.getxAxisData());
		System.out.println(s2.getyAxisData());
		
		System.out.println(s.getxAxisData());
		System.out.println(s.getyAxisData());
		chart.addSeries("Percentage Filled Group - Saturdays", 
				s1.getxAxisData(), s1.getyAxisData()).setFillColor(Color.red);
		chart.addSeries("Percentage Filled Group - Sundays", 
				s2.getxAxisData(), s2.getyAxisData()).setFillColor(Color.orange);
		chart.addSeries("Percentage Filled Group - Weekdays", 
				s.getxAxisData(), s.getyAxisData()).setFillColor(Color.blue);
		
		chart.getStyler().setxAxisTickLabelsFormattingFunction(d -> String.format("%.2f%%", d * 100));
		
		charts.add(chart);
		
		BitmapEncoder.saveBitmap(charts, 4, 3, "./charts/histogram_schedule-Filled", BitmapFormat.PNG);
		BitmapEncoder.saveBitmap(chart, "./charts/histogram_routes", BitmapFormat.PNG);
	}

}
