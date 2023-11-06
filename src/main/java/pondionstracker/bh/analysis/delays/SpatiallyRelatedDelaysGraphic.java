package pondionstracker.bh.analysis.delays;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.List;

import org.knowm.xchart.BitmapEncoder;
import org.knowm.xchart.BitmapEncoder.BitmapFormat;
import org.knowm.xchart.SwingWrapper;
import org.knowm.xchart.XYChart;
import org.knowm.xchart.XYChartBuilder;
import org.knowm.xchart.style.markers.SeriesMarkers;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.SneakyThrows;

public class SpatiallyRelatedDelaysGraphic {
	
	private record YInfoRecord(String t, String x, List<Double> y, Color c) {}
	
	@SneakyThrows
	public static void main(String[] args) {
		var mapper = new ObjectMapper();
		mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
		
		List<String> days = List.of("Weekdays", "Saturday", "Sunday");
		var charts = days
			.stream()
			.map(s -> {
				try {
					var ys = mapper.readValue(new File("./data/ydelays_ytrips_%s.json".formatted(s.toUpperCase())), 
							new TypeReference<List<List<Double>>>() {
					});
					return List.of(new YInfoRecord(s, "Bus Stop", ys.get(0), Color.blue), 
							new YInfoRecord(s, "Trip", ys.get(1), Color.orange));
					
				} catch (IOException e) {
					return null;
				}
			})
			.flatMap(List<YInfoRecord>::stream)
			.map(value -> {
				XYChart chart = new XYChartBuilder()
						.title("%s".formatted(value.t))
						.xAxisTitle(value.x)
						.yAxisTitle("DELAY's Percentage")
						.width(300)
						.height(200)
						.build();
				chart.getStyler().setYAxisMin(0d);
			    chart.getStyler().setYAxisMax(100d);
			    chart.getStyler().setLegendVisible(false);
			    chart.getStyler().setXAxisDecimalPattern("0.###");
			    
			    chart.getStyler().setSeriesColors(new Color[] {value.c});
			    
			    
			    
			    
			    chart.addSeries(value.t, null, value.y()).setMarker(SeriesMarkers.NONE);
			    
			    chart.getStyler().setXAxisLabelRotation(25);
			    
			    return chart;
			})
			.toList();
		
		
		

		BitmapEncoder.saveBitmap(charts, days.size(), 2, "./charts/trips_delays_per_bus_stops", BitmapFormat.PNG);
	    new SwingWrapper<XYChart>(charts).displayChartMatrix();
	}
	
	public static double calculateStandardDeviation(double[] array) {

	    // get the sum of array
	    double sum = 0.0;
	    for (double i : array) {
	        sum += i;
	    }

	    // get the mean of array
	    int length = array.length;
	    double mean = sum / length;

	    // calculate the standard deviation
	    double standardDeviation = 0.0;
	    for (double num : array) {
	        standardDeviation += Math.pow(num - mean, 2);
	    }

	    return Math.sqrt(standardDeviation / length);
	}

}
