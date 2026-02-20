-- Add IRT (Item Response Theory) parameters to item_statistics
-- 2PL model: P(correct | theta) = 1 / (1 + exp(-a * (theta - b)))

ALTER TABLE item_statistics ADD COLUMN irt_discrimination DECIMAL(6,4);
ALTER TABLE item_statistics ADD COLUMN irt_difficulty DECIMAL(6,4);
ALTER TABLE item_statistics ADD COLUMN irt_guessing DECIMAL(5,4);

COMMENT ON COLUMN item_statistics.irt_discrimination IS '2PL discrimination parameter (a). Range typically 0.5-2.5';
COMMENT ON COLUMN item_statistics.irt_difficulty IS '2PL difficulty parameter (b). Range typically -3 to +3';
COMMENT ON COLUMN item_statistics.irt_guessing IS '3PL guessing parameter (c). Fixed at 0 for 2PL. Reserved for future use';
