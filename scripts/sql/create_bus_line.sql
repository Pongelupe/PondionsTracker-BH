CREATE TABLE pondionstracker.bus_line (
	id_linha int4 NOT NULL,
	codigo_linha varchar(15) NULL,
	descricao_linha varchar(70) NULL,
	CONSTRAINT bus_line_pkey PRIMARY KEY (id_linha)
);
