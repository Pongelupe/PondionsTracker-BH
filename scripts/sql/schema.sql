-- bh.real_time_bus definition
BEGIN;
CREATE SCHEMA IF NOT EXISTS "pondionstracker-bh";
SET search_path TO "pondionstracker-bh", public;

CREATE TABLE IF NOT EXISTS real_time_bus (
	id serial NOT NULL,
	dt_entry timestamp NOT NULL,
	coord geometry NOT NULL,
	id_vehicle varchar(35) NOT NULL,
	id_line varchar(35) NOT NULL,
	current_distance_traveled int4 NOT NULL
);
CREATE INDEX IF NOT EXISTS real_time_bus_dt_entry_idx ON real_time_bus USING btree (dt_entry);
CREATE INDEX IF NOT EXISTS real_time_bus_id_vehicle_idx ON real_time_bus USING btree (id_vehicle, dt_entry);

-- Create new table to store the shape geometries
CREATE TABLE IF NOT EXISTS shapes_summarized (
  shape_id text not null,
  length numeric(12, 2) not null,
  shape_ls geometry(LineString, 4326) not null,
  CONSTRAINT shape_geom_pkey PRIMARY KEY (shape_id)
);

COMMIT;
