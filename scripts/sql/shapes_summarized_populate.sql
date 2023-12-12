/**
Insert geometries into shapes_summarized by summarizing the shapes table
*/
SET search_path TO "pondionstracker-bh", public;
INSERT INTO shapes_summarized (shape_id, length, shape_ls)
SELECT shape_id, ST_Length(shape_ls::geography), shape_ls
FROM (
  SELECT
    shape_id,
    ST_MakeLine(shape_pt_loc::geometry order by shape_pt_sequence) AS shape_ls
  FROM shapes 
  GROUP BY 1
) a;
