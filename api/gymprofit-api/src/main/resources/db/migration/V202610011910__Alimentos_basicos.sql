-- GP-127 · Alimentos básicos: Ciqual 2025 (ANSES, Licence Ouverte / Etalab 2.0) y
-- USDA FoodData Central SR Legacy (dominio público). GENERADO por
-- datos/basicos/generar_basicos.py desde datos/basicos/basicos.csv: no se edita a mano.
-- Idempotente por (fuente, codigo_origen): volver a pasarla no duplica nada.

INSERT INTO alimentos (nombre, nombre_en, categoria, categoria_en, calorias, proteinas,
                       carbohidratos, grasas, fibra, porcion_gramos, activo, fuente,
                       codigo_origen, revisado)
VALUES
    ('Pechuga de pollo, cruda', 'Chicken breast, raw', 'Carnes y aves', 'Meat & poultry', 110, 23.40, 0.00, 1.50, 0.00, 100, 1, 'CIQUAL', '36017', 1),
    ('Pechuga de pollo, a la plancha', 'Chicken breast, grilled', 'Carnes y aves', 'Meat & poultry', 141, 30.10, 0.00, 2.00, 0.00, 100, 1, 'CIQUAL', '36018', 1),
    ('Muslo de pollo sin piel, crudo', 'Chicken thigh, skinless, raw', 'Carnes y aves', 'Meat & poultry', 114, 19.30, 0.00, 4.05, 0.00, 100, 1, 'CIQUAL', '36024', 1),
    ('Muslo de pollo sin piel, asado', 'Chicken thigh, skinless, roasted', 'Carnes y aves', 'Meat & poultry', 171, 24.80, 0.00, 8.03, 0.00, 100, 1, 'CIQUAL', '36006', 1),
    ('Muslo de pollo con piel, crudo', 'Chicken thigh with skin, raw', 'Carnes y aves', 'Meat & poultry', 192, 17.30, 0.00, 13.50, 0.00, 100, 1, 'CIQUAL', '36002', 1),
    ('Alitas de pollo, crudas', 'Chicken wings, raw', 'Carnes y aves', 'Meat & poultry', 181, 20.40, 0.00, 11.00, 0.00, 100, 1, 'CIQUAL', '36023', 1),
    ('Pollo asado con piel', 'Roast chicken with skin', 'Carnes y aves', 'Meat & poultry', 204, 28.90, 0.00, 9.88, 0.00, 100, 1, 'CIQUAL', '36005', 1),
    ('Pechuga de pavo, cruda', 'Turkey breast, raw', 'Carnes y aves', 'Meat & poultry', 108, 23.70, 0.00, 1.48, 0.00, 100, 1, 'CIQUAL', '36304', 1),
    ('Pechuga de pavo, a la plancha', 'Turkey breast, grilled', 'Carnes y aves', 'Meat & poultry', 124, 28.50, 0.00, 1.09, 0.00, 100, 1, 'CIQUAL', '36306', 1),
    ('Muslo de pavo sin piel, crudo', 'Turkey thigh, skinless, raw', 'Carnes y aves', 'Meat & poultry', 108, 21.30, 0.00, 2.50, 0.00, 100, 1, 'CIQUAL', '36307', 1),
    ('Filete de ternera, crudo', 'Beef steak, raw', 'Carnes y aves', 'Meat & poultry', 229, 18.70, 0.06, 17.20, 0.00, 100, 1, 'CIQUAL', '6201', 1),
    ('Cadera de ternera, cruda', 'Beef rump, raw', 'Carnes y aves', 'Meat & poultry', 118, 23.50, 0.40, 2.50, 0.00, 100, 1, 'CIQUAL', '6206', 1),
    ('Cadera de ternera, a la plancha', 'Beef rump, grilled', 'Carnes y aves', 'Meat & poultry', 138, 29.00, 0.00, 2.10, 0.00, 100, 1, 'CIQUAL', '6207', 1),
    ('Solomillo de ternera, crudo', 'Beef tenderloin, raw', 'Carnes y aves', 'Meat & poultry', 132, 21.60, 0.22, 4.95, 0.00, 100, 1, 'CIQUAL', '6116', 1),
    ('Entrecot de ternera, crudo', 'Beef rib-eye, raw', 'Carnes y aves', 'Meat & poultry', 231, 19.40, 0.00, 17.10, 0.00, 100, 1, 'CIQUAL', '6103', 1),
    ('Carne picada de ternera 5 % grasa, cruda', 'Lean ground beef 5% fat, raw', 'Carnes y aves', 'Meat & poultry', 130, 21.90, 0.30, 4.59, 0.00, 100, 1, 'CIQUAL', '6250', 1),
    ('Carne picada de ternera 10 % grasa, cruda', 'Ground beef 10% fat, raw', 'Carnes y aves', 'Meat & poultry', 170, 20.00, 0.00, 10.00, 0.00, 100, 1, 'CIQUAL', '6252', 1),
    ('Carne picada de ternera 20 % grasa, cruda', 'Ground beef 20% fat, raw', 'Carnes y aves', 'Meat & poultry', 249, 17.20, 0.00, 20.00, 0.00, 100, 1, 'CIQUAL', '6256', 1),
    ('Carne picada de ternera 10 % grasa, cocinada', 'Ground beef 10% fat, cooked', 'Carnes y aves', 'Meat & poultry', 210, 26.10, 0.00, 11.80, 0.00, 100, 1, 'CIQUAL', '6253', 1),
    ('Ternera para guisar, cruda', 'Stewing beef, raw', 'Carnes y aves', 'Meat & poultry', 164, 24.00, 0.00, 7.50, 0.00, 100, 1, 'CIQUAL', '6231', 1),
    ('Escalope de ternera, crudo', 'Veal escalope, raw', 'Carnes y aves', 'Meat & poultry', 114, 20.50, 0.58, 3.28, 0.00, 100, 1, 'CIQUAL', '6521', 1),
    ('Solomillo de cerdo, crudo', 'Pork tenderloin, raw', 'Carnes y aves', 'Meat & poultry', 123, 21.20, 0.40, 4.09, 0.00, 100, 1, 'CIQUAL', '28204', 1),
    ('Solomillo de cerdo, cocinado', 'Pork tenderloin, cooked', 'Carnes y aves', 'Meat & poultry', 168, 26.10, 0.00, 7.10, 0.00, 100, 1, 'CIQUAL', '28203', 1),
    ('Chuleta de cerdo, cruda', 'Pork chop, raw', 'Carnes y aves', 'Meat & poultry', 164, 19.80, 0.38, 9.30, 0.00, 100, 1, 'CIQUAL', '28100', 1),
    ('Chuleta de cerdo, a la plancha', 'Pork chop, grilled', 'Carnes y aves', 'Meat & poultry', 211, 29.60, 0.00, 10.30, 0.00, 100, 1, 'CIQUAL', '28101', 1),
    ('Aguja de cerdo, cruda', 'Pork shoulder, raw', 'Carnes y aves', 'Meat & poultry', 238, 17.10, 0.10, 18.80, 0.00, 100, 1, 'CIQUAL', '28302', 1),
    ('Panceta de cerdo, cruda', 'Pork belly, raw', 'Carnes y aves', 'Meat & poultry', 253, 17.00, 0.00, 20.50, 0.00, 100, 1, 'CIQUAL', '28002', 1),
    ('Carne picada de cerdo, cruda', 'Ground pork, raw', 'Carnes y aves', 'Meat & poultry', 187, 18.90, 0.23, 12.30, 0.00, 100, 1, 'CIQUAL', '28472', 1),
    ('Chuletas de cordero, crudas', 'Lamb chops, raw', 'Carnes y aves', 'Meat & poultry', 262, 16.60, 0.35, 21.50, 0.00, 100, 1, 'CIQUAL', '21521', 1),
    ('Pierna de cordero, cruda', 'Leg of lamb, raw', 'Carnes y aves', 'Meat & poultry', 128, 20.00, 0.48, 5.13, 0.00, 100, 1, 'CIQUAL', '21502', 1),
    ('Conejo, crudo', 'Rabbit, raw', 'Carnes y aves', 'Meat & poultry', 188, 20.40, 0.58, 11.60, 0.00, 100, 1, 'CIQUAL', '34001', 1),
    ('Conejo, cocinado', 'Rabbit, cooked', 'Carnes y aves', 'Meat & poultry', 167, 20.50, 0.50, 9.20, 0.00, 100, 1, 'CIQUAL', '34002', 1),
    ('Magret de pato, crudo', 'Duck breast, raw', 'Carnes y aves', 'Meat & poultry', 337, 17.90, 0.10, 29.40, 0.00, 100, 1, 'CIQUAL', '36206', 1),
    ('Codorniz, cruda', 'Quail, raw', 'Carnes y aves', 'Meat & poultry', 128, 21.80, 0.00, 4.53, 0.00, 100, 1, 'CIQUAL', '36101', 1),
    ('Carne de caballo, cruda', 'Horse meat, raw', 'Carnes y aves', 'Meat & poultry', 132, 20.20, 0.50, 5.50, 0.00, 100, 1, 'CIQUAL', '6900', 1),
    ('Hígado de pollo, crudo', 'Chicken liver, raw', 'Carnes y aves', 'Meat & poultry', 134, 21.50, 1.25, 4.75, 0.00, 100, 1, 'CIQUAL', '40108', 1),
    ('Hígado de ternera, crudo', 'Calf liver, raw', 'Carnes y aves', 'Meat & poultry', 120, 15.50, 5.64, 3.40, 0.00, 100, 1, 'CIQUAL', '40106', 1),
    ('Jamón serrano', 'Serrano ham', 'Carnes y aves', 'Meat & poultry', 235, 30.40, 0.76, 12.30, 0.00, 100, 1, 'CIQUAL', '28845', 1),
    ('Jamón cocido', 'Cooked ham', 'Carnes y aves', 'Meat & poultry', 125, 19.50, 1.70, 4.50, 0.08, 100, 1, 'CIQUAL', '28910', 1),
    ('Jamón cocido extra', 'Premium cooked ham', 'Carnes y aves', 'Meat & poultry', 113, 21.00, 0.76, 2.83, 0.10, 100, 1, 'CIQUAL', '28900', 1),
    ('Pechuga de pavo en lonchas', 'Sliced turkey breast', 'Carnes y aves', 'Meat & poultry', 99, 20.90, 0.02, 1.67, 0.39, 100, 1, 'CIQUAL', '28964', 1),
    ('Pechuga de pollo en lonchas', 'Sliced chicken breast', 'Carnes y aves', 'Meat & poultry', 105, 20.70, 1.39, 1.79, 0.53, 100, 1, 'CIQUAL', '28963', 1),
    ('Chorizo', 'Chorizo', 'Carnes y aves', 'Meat & poultry', 425, 23.50, 1.24, 36.10, 0.93, 100, 1, 'CIQUAL', '30315', 1),
    ('Salchichón o fuet', 'Dry-cured sausage (salchichón, fuet)', 'Carnes y aves', 'Meat & poultry', 418, 24.20, 5.48, 33.10, 0.73, 100, 1, 'CIQUAL', '30300', 1),
    ('Salami', 'Salami', 'Carnes y aves', 'Meat & poultry', 457, 17.70, 2.59, 41.80, 0.00, 100, 1, 'CIQUAL', '30350', 1),
    ('Mortadela', 'Mortadella', 'Carnes y aves', 'Meat & poultry', 310, 15.20, 0.69, 27.40, 0.00, 100, 1, 'CIQUAL', '30789', 1),
    ('Morcilla', 'Black pudding', 'Carnes y aves', 'Meat & poultry', 297, 12.20, 0.29, 27.10, 1.42, 100, 1, 'CIQUAL', '8703', 1),
    ('Salchichas de pollo tipo Frankfurt', 'Chicken frankfurters', 'Carnes y aves', 'Meat & poultry', 236, 14.00, 1.82, 18.90, 0.00, 100, 1, 'CIQUAL', '30131', 1),
    ('Salchicha fresca de cerdo, cruda', 'Fresh pork sausage, raw', 'Carnes y aves', 'Meat & poultry', 321, 13.30, 0.64, 29.50, 0.00, 100, 1, 'CIQUAL', '30110', 1),
    ('Bacon ahumado', 'Smoked bacon', 'Carnes y aves', 'Meat & poultry', 303, 15.60, 0.79, 26.10, 1.00, 100, 1, 'CIQUAL', '28502', 1),
    ('Lomo de cerdo curado', 'Cured pork loin', 'Carnes y aves', 'Meat & poultry', 118, 23.10, 0.70, 2.60, 0.00, 100, 1, 'CIQUAL', '28727', 1),
    ('Paté de hígado', 'Liver pâté', 'Carnes y aves', 'Meat & poultry', 316, 13.70, 1.21, 28.30, 0.84, 100, 1, 'CIQUAL', '8316', 1),
    ('Nuggets de pollo', 'Chicken nuggets', 'Carnes y aves', 'Meat & poultry', 250, 15.60, 19.40, 11.60, 2.40, 100, 1, 'CIQUAL', '36027', 1),
    ('Filete de pavo empanado', 'Breaded turkey cutlet', 'Carnes y aves', 'Meat & poultry', 211, 14.10, 13.50, 10.90, 1.30, 100, 1, 'CIQUAL', '36318', 1),
    ('Albóndigas de ternera, cocinadas', 'Beef meatballs, cooked', 'Carnes y aves', 'Meat & poultry', 217, 18.50, 5.65, 13.30, 0.63, 100, 1, 'CIQUAL', '25163', 1),
    ('Salmón, crudo', 'Salmon, raw', 'Pescado y marisco', 'Fish & seafood', 193, 20.50, 0.00, 12.40, 0.12, 100, 1, 'CIQUAL', '26036', 1),
    ('Salmón, al horno', 'Salmon, baked', 'Pescado y marisco', 'Fish & seafood', 214, 22.10, 0.98, 13.50, 0.00, 100, 1, 'CIQUAL', '26230', 1),
    ('Salmón ahumado', 'Smoked salmon', 'Pescado y marisco', 'Fish & seafood', 184, 22.20, 0.13, 10.50, 0.26, 100, 1, 'CIQUAL', '26037', 1),
    ('Atún al natural, en lata', 'Canned tuna in water', 'Pescado y marisco', 'Fish & seafood', 143, 26.80, 0.00, 3.94, 0.00, 100, 1, 'CIQUAL', '26039', 1),
    ('Atún en aceite, en lata', 'Canned tuna in oil', 'Pescado y marisco', 'Fish & seafood', 206, 23.30, 0.76, 12.20, 0.00, 100, 1, 'CIQUAL', '26180', 1),
    ('Atún fresco, crudo', 'Fresh tuna, raw', 'Pescado y marisco', 'Fish & seafood', 155, 24.00, 2.72, 5.38, 0.00, 100, 1, 'CIQUAL', '26053', 1),
    ('Merluza, cruda', 'Hake, raw', 'Pescado y marisco', 'Fish & seafood', 83, 17.60, 0.00, 1.35, 0.00, 100, 1, 'CIQUAL', '26044', 1),
    ('Merluza, cocinada', 'Hake, cooked', 'Pescado y marisco', 'Fish & seafood', 112, 21.20, 0.00, 3.00, 0.00, 100, 1, 'CIQUAL', '26120', 1),
    ('Bacalao fresco, crudo', 'Cod, raw', 'Pescado y marisco', 'Fish & seafood', 77, 18.10, 0.00, 0.57, 0.10, 100, 1, 'CIQUAL', '26043', 1),
    ('Bacalao salado, seco', 'Salt cod, dried', 'Pescado y marisco', 'Fish & seafood', 210, 47.60, 1.10, 1.67, 0.00, 100, 1, 'CIQUAL', '26098', 1),
    ('Bacalao desalado, cocido', 'Salt cod, desalted and boiled', 'Pescado y marisco', 'Fish & seafood', 113, 26.00, 0.00, 1.01, 0.00, 100, 1, 'CIQUAL', '26024', 1),
    ('Sardinas, crudas', 'Sardines, raw', 'Pescado y marisco', 'Fish & seafood', 160, 19.50, 0.00, 9.17, 0.00, 100, 1, 'CIQUAL', '26065', 1),
    ('Sardinas en aceite de oliva, en lata', 'Canned sardines in olive oil', 'Pescado y marisco', 'Fish & seafood', 224, 23.30, 0.11, 14.50, 0.00, 100, 1, 'CIQUAL', '26040', 1),
    ('Caballa, cruda', 'Mackerel, raw', 'Pescado y marisco', 'Fish & seafood', 198, 18.10, 0.93, 13.50, 0.00, 100, 1, 'CIQUAL', '26051', 1),
    ('Boquerones, crudos', 'Anchovies, raw', 'Pescado y marisco', 'Fish & seafood', 129, 18.60, 0.00, 6.07, 0.00, 100, 1, 'CIQUAL', '26079', 1),
    ('Anchoas en aceite de oliva', 'Anchovy fillets in olive oil', 'Pescado y marisco', 'Fish & seafood', 197, 25.00, 1.36, 9.90, 1.10, 100, 1, 'CIQUAL', '26694', 1),
    ('Dorada, cruda', 'Gilthead sea bream, raw', 'Pescado y marisco', 'Fish & seafood', 129, 20.90, 0.00, 5.10, 0.00, 100, 1, 'CIQUAL', '26088', 1),
    ('Carbonero, crudo', 'Coley, raw', 'Pescado y marisco', 'Fish & seafood', 82, 18.80, 0.00, 0.80, 0.00, 100, 1, 'CIQUAL', '26134', 1),
    ('Abadejo, crudo', 'Pollock, raw', 'Pescado y marisco', 'Fish & seafood', 73, 16.30, 0.51, 0.61, 0.09, 100, 1, 'CIQUAL', '26006', 1),
    ('Lenguado, crudo', 'Sole, raw', 'Pescado y marisco', 'Fish & seafood', 77, 18.00, 0.00, 0.60, 0.00, 100, 1, 'CIQUAL', '26058', 1),
    ('Trucha, cruda', 'Trout, raw', 'Pescado y marisco', 'Fish & seafood', 132, 19.00, 0.00, 6.24, 0.00, 100, 1, 'CIQUAL', '27009', 1),
    ('Rape, crudo', 'Monkfish, raw', 'Pescado y marisco', 'Fish & seafood', 67, 15.10, 0.00, 0.74, 0.10, 100, 1, 'CIQUAL', '26018', 1),
    ('Rodaballo, crudo', 'Turbot, raw', 'Pescado y marisco', 'Fish & seafood', 86, 17.90, 0.00, 1.55, 0.00, 100, 1, 'CIQUAL', '26174', 1),
    ('Pez espada, crudo', 'Swordfish, raw', 'Pescado y marisco', 'Fish & seafood', 134, 18.90, 1.00, 5.97, 0.00, 100, 1, 'CIQUAL', '26082', 1),
    ('Arenque, crudo', 'Herring, raw', 'Pescado y marisco', 'Fish & seafood', 176, 17.70, 0.00, 11.70, 0.00, 100, 1, 'CIQUAL', '26011', 1),
    ('Gambas, crudas', 'Prawns, raw', 'Pescado y marisco', 'Fish & seafood', 99, 19.70, 3.21, 0.84, 0.00, 100, 1, 'CIQUAL', '10021', 1),
    ('Gambas, cocidas', 'Prawns, cooked', 'Pescado y marisco', 'Fish & seafood', 91, 20.50, 0.20, 0.79, 0.00, 100, 1, 'CIQUAL', '10007', 1),
    ('Cigalas, crudas', 'Langoustines, raw', 'Pescado y marisco', 'Fish & seafood', 90, 19.10, 1.53, 0.79, 0.00, 100, 1, 'CIQUAL', '10024', 1),
    ('Mejillones, crudos', 'Mussels, raw', 'Pescado y marisco', 'Fish & seafood', 72, 11.20, 2.69, 1.82, 0.00, 100, 1, 'CIQUAL', '10014', 1),
    ('Mejillones, cocidos', 'Mussels, cooked', 'Pescado y marisco', 'Fish & seafood', 108, 17.20, 5.12, 2.09, 0.00, 100, 1, 'CIQUAL', '10013', 1),
    ('Calamar, crudo', 'Squid, raw', 'Pescado y marisco', 'Fish & seafood', 77, 14.40, 2.17, 1.19, 0.00, 100, 1, 'CIQUAL', '10001', 1),
    ('Calamares a la romana', 'Battered squid rings', 'Pescado y marisco', 'Fish & seafood', 229, 8.69, 26.70, 9.50, 1.00, 100, 1, 'CIQUAL', '10002', 1),
    ('Pulpo, crudo', 'Octopus, raw', 'Pescado y marisco', 'Fish & seafood', 60, 12.90, 0.97, 0.50, 0.00, 100, 1, 'CIQUAL', '10018', 1),
    ('Pulpo, cocido', 'Octopus, cooked', 'Pescado y marisco', 'Fish & seafood', 156, 29.80, 4.40, 2.08, 0.00, 100, 1, 'CIQUAL', '10079', 1),
    ('Sepia, cruda', 'Cuttlefish, raw', 'Pescado y marisco', 'Fish & seafood', 76, 16.20, 0.51, 1.05, 0.00, 100, 1, 'CIQUAL', '10016', 1),
    ('Almejas, crudas', 'Clams, raw', 'Pescado y marisco', 'Fish & seafood', 80, 11.50, 2.66, 2.65, 0.00, 100, 1, 'CIQUAL', '10017', 1),
    ('Surimi', 'Surimi', 'Pescado y marisco', 'Fish & seafood', 117, 7.54, 11.80, 4.33, 0.73, 100, 1, 'CIQUAL', '26046', 1),
    ('Varitas de pescado empanadas', 'Breaded fish fingers', 'Pescado y marisco', 'Fish & seafood', 255, 13.20, 18.40, 13.70, 2.50, 100, 1, 'CIQUAL', '26028', 1),
    ('Huevo', 'Egg', 'Huevos', 'Eggs', 140, 12.80, 0.06, 9.83, 0.00, 100, 1, 'CIQUAL', '22000', 1),
    ('Huevo duro', 'Hard-boiled egg', 'Huevos', 'Eggs', 134, 13.50, 0.52, 8.62, 0.00, 100, 1, 'CIQUAL', '22010', 1),
    ('Huevo escalfado', 'Poached egg', 'Huevos', 'Eggs', 139, 12.50, 0.89, 9.47, 0.00, 100, 1, 'CIQUAL', '22011', 1),
    ('Huevo a la plancha', 'Fried egg, no oil', 'Huevos', 'Eggs', 157, 14.60, 1.40, 10.40, 0.00, 100, 1, 'CIQUAL', '22505', 1),
    ('Huevo frito', 'Fried egg', 'Huevos', 'Eggs', 196, 13.61, 0.83, 14.84, 0.00, 100, 1, 'USDA', '173423', 1),
    ('Huevos revueltos', 'Scrambled eggs', 'Huevos', 'Eggs', 167, 11.90, 0.44, 13.00, 0.00, 100, 1, 'CIQUAL', '22502', 1),
    ('Clara de huevo', 'Egg white', 'Huevos', 'Eggs', 48, 10.90, 0.73, 0.17, 0.00, 100, 1, 'CIQUAL', '22001', 1),
    ('Yema de huevo', 'Egg yolk', 'Huevos', 'Eggs', 307, 15.50, 1.09, 26.70, 0.00, 100, 1, 'CIQUAL', '22002', 1),
    ('Tortilla francesa', 'Plain omelette', 'Huevos', 'Eggs', 196, 13.80, 0.90, 15.30, 0.00, 100, 1, 'CIQUAL', '22500', 1),
    ('Tortilla de patatas', 'Spanish potato omelette', 'Huevos', 'Eggs', 165, 5.56, 11.70, 9.90, 2.70, 100, 1, 'CIQUAL', '22510', 1),
    ('Huevo de codorniz', 'Quail egg', 'Huevos', 'Eggs', 154, 13.10, 0.41, 11.10, 0.00, 100, 1, 'CIQUAL', '22050', 1),
    ('Leche semidesnatada', 'Semi-skimmed milk', 'Lácteos', 'Dairy', 48, 3.41, 5.00, 1.56, 0.00, 100, 1, 'CIQUAL', '19041', 1),
    ('Leche entera', 'Whole milk', 'Lácteos', 'Dairy', 65, 3.39, 4.77, 3.55, 0.00, 100, 1, 'CIQUAL', '19023', 1),
    ('Leche desnatada', 'Skimmed milk', 'Lácteos', 'Dairy', 34, 3.53, 5.01, 0.02, 0.00, 100, 1, 'CIQUAL', '19050', 1),
    ('Leche sin lactosa semidesnatada', 'Lactose-free semi-skimmed milk', 'Lácteos', 'Dairy', 44, 3.38, 4.69, 1.29, 0.00, 100, 1, 'CIQUAL', '19060', 1),
    ('Yogur natural', 'Plain yogurt', 'Lácteos', 'Dairy', 50, 3.75, 4.26, 1.68, 0.00, 100, 1, 'CIQUAL', '19593', 1),
    ('Yogur natural desnatado', 'Plain fat-free yogurt', 'Lácteos', 'Dairy', 39, 4.08, 4.89, 0.05, 0.03, 100, 1, 'CIQUAL', '19594', 1),
    ('Yogur natural azucarado', 'Sweetened plain yogurt', 'Lácteos', 'Dairy', 82, 2.86, 10.70, 2.81, 0.03, 100, 1, 'CIQUAL', '19599', 1),
    ('Yogur de sabores', 'Flavoured yogurt', 'Lácteos', 'Dairy', 86, 3.09, 12.70, 2.25, 0.02, 100, 1, 'CIQUAL', '19575', 1),
    ('Yogur con frutas', 'Fruit yogurt', 'Lácteos', 'Dairy', 89, 3.17, 12.60, 2.41, 0.60, 100, 1, 'CIQUAL', '19587', 1),
    ('Yogur griego natural', 'Plain Greek-style yogurt', 'Lácteos', 'Dairy', 103, 2.96, 3.73, 8.16, 0.07, 100, 1, 'CIQUAL', '19860', 1),
    ('Yogur griego natural desnatado', 'Plain non-fat Greek yogurt', 'Lácteos', 'Dairy', 59, 10.19, 3.60, 0.39, 0.00, 100, 1, 'USDA', '170894', 1),
    ('Yogur alto en proteínas, natural', 'High-protein plain yogurt', 'Lácteos', 'Dairy', 74, 9.10, 3.45, 2.70, 0.05, 100, 1, 'CIQUAL', '19724', 1),
    ('Kéfir', 'Kefir', 'Lácteos', 'Dairy', 61, 3.13, 4.50, 3.40, 0.00, 100, 1, 'CIQUAL', '19865', 1),
    ('Queso batido desnatado', 'Fat-free quark', 'Lácteos', 'Dairy', 48, 7.19, 4.22, 0.05, 0.04, 100, 1, 'CIQUAL', '19644', 1),
    ('Queso batido natural', 'Plain quark', 'Lácteos', 'Dairy', 76, 7.13, 3.86, 3.23, 0.02, 100, 1, 'CIQUAL', '19646', 1),
    ('Queso cottage', 'Cottage cheese', 'Lácteos', 'Dairy', 72, 12.39, 2.72, 1.02, 0.00, 100, 1, 'USDA', '173417', 1),
    ('Queso fresco para untar', 'Cream cheese spread', 'Lácteos', 'Dairy', 225, 6.13, 3.31, 20.70, 0.00, 100, 1, 'CIQUAL', '12068', 1),
    ('Queso fresco para untar, light', 'Light cream cheese spread', 'Lácteos', 'Dairy', 133, 10.60, 5.12, 7.48, 0.25, 100, 1, 'CIQUAL', '12069', 1),
    ('Requesón', 'Ricotta', 'Lácteos', 'Dairy', 138, 11.39, 5.14, 7.91, 0.00, 100, 1, 'USDA', '171248', 1),
    ('Mozzarella', 'Mozzarella', 'Lácteos', 'Dairy', 227, 16.10, 0.70, 17.70, 0.00, 100, 1, 'CIQUAL', '19590', 1),
    ('Burrata', 'Burrata', 'Lácteos', 'Dairy', 259, 10.60, 0.93, 23.40, 0.00, 100, 1, 'CIQUAL', '12072', 1),
    ('Parmesano', 'Parmesan', 'Lácteos', 'Dairy', 411, 30.50, 1.14, 31.00, 0.00, 100, 1, 'CIQUAL', '12120', 1),
    ('Queso de oveja curado', 'Cured sheep''s cheese', 'Lácteos', 'Dairy', 383, 25.00, 1.00, 31.00, 0.00, 100, 1, 'CIQUAL', '12122', 1),
    ('Queso de oveja semicurado', 'Semi-cured sheep''s cheese', 'Lácteos', 'Dairy', 393, 21.80, 0.72, 33.60, 0.00, 100, 1, 'CIQUAL', '12747', 1),
    ('Emmental', 'Emmental', 'Lácteos', 'Dairy', 373, 27.30, 0.00, 28.80, 0.00, 100, 1, 'CIQUAL', '12115', 1),
    ('Gouda', 'Gouda', 'Lácteos', 'Dairy', 369, 24.20, 0.00, 30.20, 0.00, 100, 1, 'CIQUAL', '12736', 1),
    ('Edam', 'Edam', 'Lácteos', 'Dairy', 320, 25.20, 0.00, 24.40, 0.00, 100, 1, 'CIQUAL', '12729', 1),
    ('Cheddar', 'Cheddar', 'Lácteos', 'Dairy', 399, 24.20, 1.20, 32.60, 0.00, 100, 1, 'CIQUAL', '12726', 1),
    ('Brie', 'Brie', 'Lácteos', 'Dairy', 345, 17.30, 0.68, 30.40, 0.06, 100, 1, 'CIQUAL', '12020', 1),
    ('Camembert', 'Camembert', 'Lácteos', 'Dairy', 280, 19.10, 0.00, 22.50, 0.00, 100, 1, 'CIQUAL', '12001', 1),
    ('Rulo de cabra', 'Goat''s cheese log', 'Lácteos', 'Dairy', 285, 18.40, 0.00, 23.30, 0.00, 100, 1, 'CIQUAL', '12812', 1),
    ('Queso feta', 'Feta cheese', 'Lácteos', 'Dairy', 284, 14.80, 0.45, 24.30, 0.20, 100, 1, 'CIQUAL', '12066', 1),
    ('Queso en lonchas fundido', 'Processed cheese slices', 'Lácteos', 'Dairy', 246, 13.80, 5.34, 18.50, 0.00, 100, 1, 'CIQUAL', '12300', 1),
    ('Quesitos', 'Processed cheese triangles', 'Lácteos', 'Dairy', 238, 11.00, 8.52, 17.60, 0.13, 100, 1, 'CIQUAL', '12303', 1),
    ('Queso rallado', 'Grated cheese', 'Lácteos', 'Dairy', 386, 31.40, 0.15, 28.90, 0.00, 100, 1, 'CIQUAL', '12775', 1),
    ('Mantequilla', 'Butter', 'Lácteos', 'Dairy', 753, 0.63, 0.71, 83.00, 0.00, 100, 1, 'CIQUAL', '16400', 1),
    ('Mantequilla light', 'Light butter', 'Lácteos', 'Dairy', 390, 0.25, 3.60, 41.70, 0.00, 100, 1, 'CIQUAL', '16415', 1),
    ('Nata para cocinar 30 %', 'Cooking cream 30%', 'Lácteos', 'Dairy', 293, 2.16, 3.69, 30.00, 0.00, 100, 1, 'CIQUAL', '19417', 1),
    ('Nata ligera 15 %', 'Light cream 15%', 'Lácteos', 'Dairy', 173, 2.90, 4.30, 16.00, 0.00, 100, 1, 'CIQUAL', '19436', 1),
    ('Nata para montar', 'Heavy whipping cream', 'Lácteos', 'Dairy', 340, 2.84, 2.84, 36.08, 0.00, 100, 1, 'USDA', '170859', 1),
    ('Flan de huevo', 'Egg custard flan', 'Lácteos', 'Dairy', 113, 4.14, 17.70, 2.74, 0.38, 100, 1, 'CIQUAL', '19674', 1),
    ('Natillas de vainilla', 'Vanilla custard', 'Lácteos', 'Dairy', 103, 2.92, 16.20, 2.88, 0.11, 100, 1, 'CIQUAL', '39229', 1),
    ('Arroz con leche', 'Rice pudding', 'Lácteos', 'Dairy', 121, 2.94, 18.90, 3.40, 1.20, 100, 1, 'CIQUAL', '39212', 1),
    ('Natillas de chocolate', 'Chocolate custard', 'Lácteos', 'Dairy', 121, 3.39, 17.90, 3.55, 1.50, 100, 1, 'CIQUAL', '39200', 1),
    ('Lentejas, secas', 'Lentils, dry', 'Legumbres', 'Legumes', 327, 25.10, 44.50, 1.80, 16.40, 100, 1, 'CIQUAL', '20585', 1),
    ('Lentejas, cocidas', 'Lentils, cooked', 'Legumbres', 'Legumes', 125, 10.10, 16.20, 0.58, 8.45, 100, 1, 'CIQUAL', '20587', 1),
    ('Lentejas rojas, secas', 'Red lentils, dry', 'Legumbres', 'Legumes', 328, 27.70, 44.90, 0.80, 15.40, 100, 1, 'CIQUAL', '20535', 1),
    ('Lentejas de bote', 'Canned lentils', 'Legumbres', 'Legumes', 90, 6.83, 12.30, 0.64, 4.00, 100, 1, 'CIQUAL', '20510', 1),
    ('Garbanzos, secos', 'Chickpeas, dry', 'Legumbres', 'Legumes', 350, 20.50, 47.50, 6.04, 12.20, 100, 1, 'CIQUAL', '20516', 1),
    ('Garbanzos, cocidos', 'Chickpeas, cooked', 'Legumbres', 'Legumes', 148, 8.31, 17.70, 3.00, 8.20, 100, 1, 'CIQUAL', '20507', 1),
    ('Garbanzos de bote', 'Canned chickpeas', 'Legumbres', 'Legumes', 122, 6.74, 15.00, 2.68, 5.45, 100, 1, 'CIQUAL', '20532', 1),
    ('Alubias blancas, secas', 'White beans, dry', 'Legumbres', 'Legumes', 307, 23.40, 43.90, 0.85, 15.20, 100, 1, 'CIQUAL', '20501', 1),
    ('Alubias blancas, cocidas', 'White beans, cooked', 'Legumbres', 'Legumes', 112, 6.75, 12.00, 1.10, 13.80, 100, 1, 'CIQUAL', '20502', 1),
    ('Alubias blancas de bote', 'Canned white beans', 'Legumbres', 'Legumes', 86, 5.88, 10.90, 0.50, 7.00, 100, 1, 'CIQUAL', '20511', 1),
    ('Alubias rojas, secas', 'Kidney beans, dry', 'Legumbres', 'Legumes', 314, 22.50, 46.10, 1.06, 15.20, 100, 1, 'CIQUAL', '20525', 1),
    ('Alubias rojas, cocidas', 'Kidney beans, cooked', 'Legumbres', 'Legumes', 116, 9.63, 12.30, 0.60, 11.60, 100, 1, 'CIQUAL', '20503', 1),
    ('Alubias rojas de bote', 'Canned kidney beans', 'Legumbres', 'Legumes', 108, 8.31, 13.00, 0.97, 7.00, 100, 1, 'CIQUAL', '20524', 1),
    ('Habas, cocidas', 'Broad beans, cooked', 'Legumbres', 'Legumes', 83, 8.06, 9.35, 0.80, 3.10, 100, 1, 'CIQUAL', '20500', 1),
    ('Guisantes, crudos', 'Green peas, raw', 'Legumbres', 'Legumes', 81, 5.42, 11.40, 0.40, 5.10, 100, 1, 'CIQUAL', '20072', 1),
    ('Guisantes congelados, cocidos', 'Frozen peas, cooked', 'Legumbres', 'Legumes', 66, 5.15, 7.95, 0.27, 5.50, 100, 1, 'CIQUAL', '20124', 1),
    ('Soja en grano', 'Soybeans', 'Legumbres', 'Legumes', 441, 40.00, 20.90, 19.90, 9.30, 100, 1, 'CIQUAL', '20901', 1),
    ('Edamame, cocido', 'Edamame, cooked', 'Legumbres', 'Legumes', 121, 11.91, 8.91, 5.20, 5.20, 100, 1, 'USDA', '168411', 1),
    ('Tofu', 'Tofu', 'Legumbres', 'Legumes', 147, 14.70, 2.87, 8.50, 0.25, 100, 1, 'CIQUAL', '20904', 1),
    ('Tempeh', 'Tempeh', 'Legumbres', 'Legumes', 192, 20.29, 7.64, 10.80, NULL, 100, 1, 'USDA', '174272', 1),
    ('Soja texturizada, hidratada', 'Textured soy protein, rehydrated', 'Legumbres', 'Legumes', 150, 20.40, 7.03, 2.90, 5.61, 100, 1, 'CIQUAL', '20591', 1),
    ('Hummus', 'Hummus', 'Legumbres', 'Legumes', 258, 8.06, 9.00, 19.90, 5.18, 100, 1, 'CIQUAL', '25621', 1),
    ('Arroz blanco, crudo', 'White rice, uncooked', 'Cereales y pan', 'Cereals & bread', 350, 7.38, 77.50, 0.79, 1.53, 100, 1, 'CIQUAL', '9100', 1),
    ('Arroz blanco, cocido', 'White rice, cooked', 'Cereales y pan', 'Cereals & bread', 155, 3.31, 33.20, 0.70, 1.40, 100, 1, 'CIQUAL', '9104', 1),
    ('Arroz integral, crudo', 'Brown rice, uncooked', 'Cereales y pan', 'Cereals & bread', 350, 7.38, 71.40, 2.80, 5.00, 100, 1, 'CIQUAL', '9102', 1),
    ('Arroz integral, cocido', 'Brown rice, cooked', 'Cereales y pan', 'Cereals & bread', 187, 4.06, 37.60, 1.80, 2.10, 100, 1, 'CIQUAL', '9103', 1),
    ('Arroz basmati, cocido', 'Basmati rice, cooked', 'Cereales y pan', 'Cereals & bread', 148, 3.19, 32.90, 0.40, 0.25, 100, 1, 'CIQUAL', '9125', 1),
    ('Pasta, cruda', 'Pasta, uncooked', 'Cereales y pan', 'Cereals & bread', 364, 13.10, 72.70, 1.60, 2.91, 100, 1, 'CIQUAL', '9810', 1),
    ('Pasta, cocida', 'Pasta, cooked', 'Cereales y pan', 'Cereals & bread', 167, 6.69, 31.40, 1.10, 2.20, 100, 1, 'CIQUAL', '9811', 1),
    ('Pasta integral, cruda', 'Whole-wheat pasta, uncooked', 'Cereales y pan', 'Cereals & bread', 353, 12.60, 67.60, 2.20, 6.10, 100, 1, 'CIQUAL', '9870', 1),
    ('Pasta integral, cocida', 'Whole-wheat pasta, cooked', 'Cereales y pan', 'Cereals & bread', 179, 7.88, 31.30, 1.40, 5.10, 100, 1, 'CIQUAL', '9871', 1),
    ('Pasta fresca al huevo, cruda', 'Fresh egg pasta, uncooked', 'Cereales y pan', 'Cereals & bread', 283, 11.10, 53.80, 1.87, 3.17, 100, 1, 'CIQUAL', '9815', 1),
    ('Pasta rellena, cocida', 'Filled pasta, cooked', 'Cereales y pan', 'Cereals & bread', 198, 7.97, 25.60, 6.48, 2.49, 100, 1, 'CIQUAL', '25216', 1),
    ('Fideos orientales, crudos', 'Asian noodles, uncooked', 'Cereales y pan', 'Cereals & bread', 371, 13.00, 71.00, 3.00, 4.00, 100, 1, 'CIQUAL', '9863', 1),
    ('Fideos de arroz, cocidos', 'Rice noodles, cooked', 'Cereales y pan', 'Cereals & bread', 87, 1.50, 20.10, 0.25, 0.25, 100, 1, 'CIQUAL', '9901', 1),
    ('Sémola de trigo, cruda', 'Wheat semolina, uncooked', 'Cereales y pan', 'Cereals & bread', 350, 13.00, 69.80, 1.28, 3.70, 100, 1, 'CIQUAL', '9610', 1),
    ('Cuscús, cocido', 'Couscous, cooked', 'Cereales y pan', 'Cereals & bread', 164, 6.38, 31.00, 0.90, 2.80, 100, 1, 'CIQUAL', '9683', 1),
    ('Quinoa, cruda', 'Quinoa, uncooked', 'Cereales y pan', 'Cereals & bread', 358, 14.10, 58.10, 6.07, 7.00, 100, 1, 'CIQUAL', '9340', 1),
    ('Quinoa, cocida', 'Quinoa, cooked', 'Cereales y pan', 'Cereals & bread', 149, 5.00, 27.90, 1.10, 3.80, 100, 1, 'CIQUAL', '9341', 1),
    ('Bulgur, cocido', 'Bulgur, cooked', 'Cereales y pan', 'Cereals & bread', 131, 4.63, 24.10, 1.00, 3.70, 100, 1, 'CIQUAL', '9691', 1),
    ('Polenta, cocida', 'Polenta, cooked', 'Cereales y pan', 'Cereals & bread', 76, 1.38, 16.90, 0.15, 1.60, 100, 1, 'CIQUAL', '9615', 1),
    ('Copos de avena', 'Rolled oats', 'Cereales y pan', 'Cereals & bread', 369, 11.40, 57.70, 7.82, 11.20, 100, 1, 'CIQUAL', '32140', 1),
    ('Muesli', 'Muesli', 'Cereales y pan', 'Cereals & bread', 433, 8.87, 63.40, 14.30, 6.90, 100, 1, 'CIQUAL', '32004', 1),
    ('Copos de maíz', 'Corn flakes', 'Cereales y pan', 'Cereals & bread', 368, 6.88, 80.00, 1.00, 5.50, 100, 1, 'CIQUAL', '32005', 1),
    ('Cereales de desayuno', 'Breakfast cereal', 'Cereales y pan', 'Cereals & bread', 402, 8.02, 73.20, 7.21, 5.99, 100, 1, 'CIQUAL', '32003', 1),
    ('Pan blanco', 'White bread', 'Cereales y pan', 'Cereals & bread', 287, 9.06, 58.30, 1.40, 2.70, 100, 1, 'CIQUAL', '7001', 1),
    ('Pan integral', 'Wholemeal bread', 'Cereales y pan', 'Cereals & bread', 234, 9.50, 41.20, 1.70, 7.30, 100, 1, 'CIQUAL', '7110', 1),
    ('Pan de pueblo', 'Country bread', 'Cereales y pan', 'Cereals & bread', 253, 8.25, 50.00, 1.30, 4.30, 100, 1, 'CIQUAL', '7100', 1),
    ('Pan con semillas', 'Seeded bread', 'Cereales y pan', 'Cereals & bread', 269, 10.50, 46.90, 3.20, 5.20, 100, 1, 'CIQUAL', '7255', 1),
    ('Pan de centeno', 'Rye bread', 'Cereales y pan', 'Cereals & bread', 260, 9.06, 51.50, 1.00, 4.50, 100, 1, 'CIQUAL', '7125', 1),
    ('Pan de molde blanco', 'White sandwich bread', 'Cereales y pan', 'Cereals & bread', 279, 7.74, 50.40, 4.37, 2.83, 100, 1, 'CIQUAL', '7200', 1),
    ('Pan de molde integral', 'Wholemeal sandwich bread', 'Cereales y pan', 'Cereals & bread', 257, 8.00, 41.80, 4.10, 7.30, 100, 1, 'CIQUAL', '7111', 1),
    ('Pan sin gluten', 'Gluten-free bread', 'Cereales y pan', 'Cereals & bread', 249, 4.81, 37.10, 6.60, 9.90, 100, 1, 'CIQUAL', '7130', 1),
    ('Pan de hamburguesa', 'Burger bun', 'Cereales y pan', 'Cereals & bread', 283, 9.00, 47.20, 4.90, 3.50, 100, 1, 'CIQUAL', '7259', 1),
    ('Pan de pita', 'Pita bread', 'Cereales y pan', 'Cereals & bread', 249, 8.20, 48.80, 1.50, 3.70, 100, 1, 'CIQUAL', '7180', 1),
    ('Tortilla de trigo', 'Wheat tortilla', 'Cereales y pan', 'Cereals & bread', 320, 8.79, 53.00, 7.48, 2.89, 100, 1, 'CIQUAL', '7815', 1),
    ('Tortilla de maíz', 'Corn tortilla', 'Cereales y pan', 'Cereals & bread', 316, 6.67, 58.00, 5.57, 3.57, 100, 1, 'CIQUAL', '7813', 1),
    ('Pan tostado', 'Crispbread toast', 'Cereales y pan', 'Cereals & bread', 394, 11.00, 73.20, 5.43, 4.32, 100, 1, 'CIQUAL', '7300', 1),
    ('Pan tostado integral', 'Wholemeal crispbread toast', 'Cereales y pan', 'Cereals & bread', 389, 12.40, 66.00, 5.90, 11.00, 100, 1, 'CIQUAL', '7340', 1),
    ('Tortitas de arroz', 'Rice cakes', 'Cereales y pan', 'Cereals & bread', 381, 8.81, 77.90, 3.00, 3.40, 100, 1, 'CIQUAL', '7352', 1),
    ('Tortitas de maíz', 'Corn cakes', 'Cereales y pan', 'Cereals & bread', 387, 7.38, 83.50, 1.90, 3.00, 100, 1, 'CIQUAL', '7354', 1),
    ('Picos de pan', 'Breadsticks', 'Cereales y pan', 'Cereals & bread', 428, 12.50, 68.80, 10.50, 4.25, 100, 1, 'CIQUAL', '7525', 1),
    ('Crackers', 'Crackers', 'Cereales y pan', 'Cereals & bread', 442, 11.40, 68.60, 12.30, 4.10, 100, 1, 'CIQUAL', '7351', 1),
    ('Harina de trigo', 'Wheat flour', 'Cereales y pan', 'Cereals & bread', 346, 11.00, 71.50, 1.00, 3.50, 100, 1, 'CIQUAL', '9435', 1),
    ('Harina integral de trigo', 'Wholemeal wheat flour', 'Cereales y pan', 'Cereals & bread', 342, 12.20, 64.90, 1.52, 10.20, 100, 1, 'CIQUAL', '9415', 1),
    ('Cruasán', 'Croissant', 'Cereales y pan', 'Cereals & bread', 412, 6.94, 47.60, 21.10, 1.80, 100, 1, 'CIQUAL', '7615', 1),
    ('Napolitana de chocolate', 'Pain au chocolat', 'Cereales y pan', 'Cereals & bread', 423, 8.19, 45.40, 22.50, 3.00, 100, 1, 'CIQUAL', '7730', 1),
    ('Brioche', 'Brioche', 'Cereales y pan', 'Cereals & bread', 363, 8.41, 52.10, 13.00, 1.94, 100, 1, 'CIQUAL', '7746', 1),
    ('Patata, cruda', 'Potato, raw', 'Cereales y pan', 'Cereals & bread', 80, 2.02, 16.20, 0.09, 2.20, 100, 1, 'CIQUAL', '4008', 1),
    ('Patata, cocida', 'Potato, boiled', 'Cereales y pan', 'Cereals & bread', 81, 1.80, 16.80, 0.34, 1.80, 100, 1, 'CIQUAL', '4003', 1),
    ('Patata, asada', 'Potato, baked', 'Cereales y pan', 'Cereals & bread', 91, 2.50, 19.00, 0.13, 2.20, 100, 1, 'CIQUAL', '4026', 1),
    ('Puré de patata', 'Mashed potatoes', 'Cereales y pan', 'Cereals & bread', 95, 1.87, 15.70, 2.40, 1.50, 100, 1, 'CIQUAL', '4018', 1),
    ('Patatas fritas, caseras', 'French fries', 'Cereales y pan', 'Cereals & bread', 258, 3.28, 32.70, 11.90, 3.90, 100, 1, 'CIQUAL', '4032', 1),
    ('Boniato, crudo', 'Sweet potato, raw', 'Cereales y pan', 'Cereals & bread', 81, 1.57, 17.10, 0.05, 3.00, 100, 1, 'CIQUAL', '4101', 1),
    ('Boniato, cocinado', 'Sweet potato, cooked', 'Cereales y pan', 'Cereals & bread', 79, 1.69, 16.30, 0.15, 2.90, 100, 1, 'CIQUAL', '4102', 1),
    ('Ñoquis de patata, cocidos', 'Potato gnocchi, cooked', 'Cereales y pan', 'Cereals & bread', 178, 5.01, 34.10, 2.05, 1.75, 100, 1, 'CIQUAL', '25510', 1),
    ('Plátano', 'Banana', 'Frutas', 'Fruits', 88, 1.06, 19.70, 0.25, 2.70, 100, 1, 'CIQUAL', '13005', 1),
    ('Manzana', 'Apple', 'Frutas', 'Fruits', 54, 0.25, 11.60, 0.25, 1.40, 100, 1, 'CIQUAL', '13039', 1),
    ('Pera', 'Pear', 'Frutas', 'Fruits', 57, 0.36, 12.30, 0.27, 2.90, 100, 1, 'CIQUAL', '13037', 1),
    ('Naranja', 'Orange', 'Frutas', 'Fruits', 42, 0.75, 8.03, 0.25, 2.70, 100, 1, 'CIQUAL', '13034', 1),
    ('Mandarina', 'Mandarin', 'Frutas', 'Fruits', 46, 0.81, 9.17, 0.25, 1.70, 100, 1, 'CIQUAL', '13024', 1),
    ('Melocotón', 'Peach', 'Frutas', 'Fruits', 38, 0.91, 7.55, 0.25, 1.50, 100, 1, 'CIQUAL', '13043', 1),
    ('Nectarina', 'Nectarine', 'Frutas', 'Fruits', 50, 0.74, 11.30, 0.25, 0.47, 100, 1, 'CIQUAL', '13399', 1),
    ('Albaricoque', 'Apricot', 'Frutas', 'Fruits', 44, 0.81, 9.01, 0.25, 1.70, 100, 1, 'CIQUAL', '13000', 1),
    ('Ciruela', 'Plum', 'Frutas', 'Fruits', 52, 0.70, 9.92, 0.28, 1.40, 100, 1, 'CIQUAL', '13100', 1),
    ('Cerezas', 'Cherries', 'Frutas', 'Fruits', 54, 0.81, 13.00, 0.15, 1.60, 100, 1, 'CIQUAL', '13008', 1),
    ('Fresas', 'Strawberries', 'Frutas', 'Fruits', 35, 0.63, 6.03, 0.25, 3.80, 100, 1, 'CIQUAL', '13014', 1),
    ('Frambuesas', 'Raspberries', 'Frutas', 'Fruits', 48, 1.19, 5.83, 0.80, 4.30, 100, 1, 'CIQUAL', '13015', 1),
    ('Arándanos', 'Blueberries', 'Frutas', 'Fruits', 58, 0.87, 10.60, 0.33, 2.40, 100, 1, 'CIQUAL', '13028', 1),
    ('Moras', 'Blackberries', 'Frutas', 'Fruits', 46, 1.13, 6.53, 0.70, 5.20, 100, 1, 'CIQUAL', '13029', 1),
    ('Uvas', 'Grapes', 'Frutas', 'Fruits', 71, 0.70, 16.30, 0.25, 1.11, 100, 1, 'CIQUAL', '13395', 1),
    ('Melón cantalupo', 'Cantaloupe melon', 'Frutas', 'Fruits', 60, 1.13, 14.80, 0.25, 1.30, 100, 1, 'CIQUAL', '13026', 1),
    ('Melón piel de sapo', 'Honeydew melon', 'Frutas', 'Fruits', 22, 0.54, 4.30, 0.14, 0.80, 100, 1, 'CIQUAL', '13742', 1),
    ('Sandía', 'Watermelon', 'Frutas', 'Fruits', 35, 0.69, 8.33, 0.25, 0.50, 100, 1, 'CIQUAL', '13036', 1),
    ('Piña', 'Pineapple', 'Frutas', 'Fruits', 52, 0.25, 11.70, 0.25, 1.20, 100, 1, 'CIQUAL', '13002', 1),
    ('Mango', 'Mango', 'Frutas', 'Fruits', 71, 0.63, 14.30, 0.25, 1.60, 100, 1, 'CIQUAL', '13025', 1),
    ('Kiwi', 'Kiwi', 'Frutas', 'Fruits', 61, 0.88, 11.00, 0.60, 2.40, 100, 1, 'CIQUAL', '13021', 1),
    ('Papaya', 'Papaya', 'Frutas', 'Fruits', 40, 0.75, 8.53, 0.15, 1.80, 100, 1, 'CIQUAL', '13035', 1),
    ('Higos', 'Figs', 'Frutas', 'Fruits', 68, 1.19, 13.50, 0.25, 4.10, 100, 1, 'CIQUAL', '13012', 1),
    ('Granada', 'Pomegranate', 'Frutas', 'Fruits', 81, 1.44, 14.30, 1.20, 2.30, 100, 1, 'CIQUAL', '13018', 1),
    ('Limón', 'Lemon', 'Frutas', 'Fruits', 24, 0.25, 1.56, 0.25, 0.25, 100, 1, 'CIQUAL', '13009', 1),
    ('Pomelo', 'Grapefruit', 'Frutas', 'Fruits', 34, 0.84, 5.92, 0.25, 1.10, 100, 1, 'CIQUAL', '13040', 1),
    ('Caqui', 'Persimmon', 'Frutas', 'Fruits', 66, 0.88, 14.30, 0.15, 3.40, 100, 1, 'CIQUAL', '13066', 1),
    ('Níspero', 'Loquat', 'Frutas', 'Fruits', 48, 0.43, 9.90, 0.20, 1.70, 100, 1, 'CIQUAL', '13564', 1),
    ('Lichi', 'Lychee', 'Frutas', 'Fruits', 78, 1.13, 16.10, 0.25, 2.00, 100, 1, 'CIQUAL', '13023', 1),
    ('Maracuyá', 'Passion fruit', 'Frutas', 'Fruits', 102, 2.13, 10.90, 3.00, 6.80, 100, 1, 'CIQUAL', '13016', 1),
    ('Aguacate', 'Avocado', 'Frutas', 'Fruits', 203, 1.56, 0.00, 20.60, 3.60, 100, 1, 'CIQUAL', '13004', 1),
    ('Coco fresco', 'Fresh coconut', 'Frutas', 'Fruits', 369, 4.09, 4.69, 34.70, 10.60, 100, 1, 'CIQUAL', '15006', 1),
    ('Dátiles', 'Dates', 'Frutas', 'Fruits', 287, 1.81, 64.70, 0.25, 7.30, 100, 1, 'CIQUAL', '13011', 1),
    ('Pasas', 'Raisins', 'Frutas', 'Fruits', 322, 3.00, 73.20, 0.90, 4.20, 100, 1, 'CIQUAL', '13046', 1),
    ('Ciruelas pasas', 'Prunes', 'Frutas', 'Fruits', 229, 1.63, 55.40, 0.40, 5.10, 100, 1, 'CIQUAL', '13042', 1),
    ('Orejones de albaricoque', 'Dried apricots', 'Frutas', 'Fruits', 239, 2.88, 59.10, 0.50, 8.30, 100, 1, 'CIQUAL', '13001', 1),
    ('Higos secos', 'Dried figs', 'Frutas', 'Fruits', 261, 2.90, 55.00, 1.22, 8.15, 100, 1, 'CIQUAL', '13013', 1),
    ('Plátano deshidratado', 'Dried banana', 'Frutas', 'Fruits', 365, 3.89, 78.40, 1.81, 9.90, 100, 1, 'CIQUAL', '13089', 1),
    ('Compota de manzana', 'Apple compote', 'Frutas', 'Fruits', 107, 0.25, 25.80, 0.23, 1.54, 100, 1, 'CIQUAL', '13038', 1),
    ('Macedonia de frutas', 'Fruit salad', 'Frutas', 'Fruits', 79, 0.61, 18.00, 0.24, 1.36, 100, 1, 'CIQUAL', '13134', 1),
    ('Aceitunas verdes', 'Green olives', 'Frutas', 'Fruits', 164, 1.13, 0.61, 16.60, 3.60, 100, 1, 'CIQUAL', '13033', 1),
    ('Aceitunas negras', 'Black olives', 'Frutas', 'Fruits', 173, 1.38, 0.05, 17.20, 6.20, 100, 1, 'CIQUAL', '13032', 1),
    ('Tomate', 'Tomato', 'Verduras', 'Vegetables', 19, 0.60, 3.69, 0.25, 1.02, 100, 1, 'CIQUAL', '20385', 1),
    ('Tomate cherry', 'Cherry tomatoes', 'Verduras', 'Vegetables', 32, 1.31, 5.62, 0.25, 1.20, 100, 1, 'CIQUAL', '20172', 1),
    ('Tomate pelado en conserva', 'Canned peeled tomatoes', 'Verduras', 'Vegetables', 16, 0.90, 1.61, 0.28, 1.27, 100, 1, 'CIQUAL', '20048', 1),
    ('Tomate triturado', 'Crushed tomatoes', 'Verduras', 'Vegetables', 48, 1.50, 8.42, 0.20, 1.60, 100, 1, 'CIQUAL', '20170', 1),
    ('Concentrado de tomate', 'Tomato paste', 'Verduras', 'Vegetables', 99, 4.40, 17.10, 0.53, 4.20, 100, 1, 'CIQUAL', '20068', 1),
    ('Pepino', 'Cucumber', 'Verduras', 'Vegetables', 17, 0.65, 2.87, 0.11, 0.50, 100, 1, 'CIQUAL', '20019', 1),
    ('Lechuga', 'Lettuce', 'Verduras', 'Vegetables', 15, 1.35, 1.22, 0.20, 1.10, 100, 1, 'CIQUAL', '20031', 1),
    ('Lechuga romana', 'Romaine lettuce', 'Verduras', 'Vegetables', 17, 1.23, 1.35, 0.30, 2.10, 100, 1, 'CIQUAL', '20171', 1),
    ('Lechuga iceberg', 'Iceberg lettuce', 'Verduras', 'Vegetables', 17, 0.95, 2.50, 0.14, 1.10, 100, 1, 'CIQUAL', '20200', 1),
    ('Rúcula', 'Rocket', 'Verduras', 'Vegetables', 28, 2.58, 2.10, 0.66, 1.60, 100, 1, 'CIQUAL', '20217', 1),
    ('Canónigos', 'Lamb''s lettuce', 'Verduras', 'Vegetables', 15, 2.00, 0.50, 0.25, 2.30, 100, 1, 'CIQUAL', '20099', 1),
    ('Espinacas, crudas', 'Spinach, raw', 'Verduras', 'Vegetables', 33, 2.68, 3.06, 0.39, 2.60, 100, 1, 'CIQUAL', '20059', 1),
    ('Espinacas, cocidas', 'Spinach, cooked', 'Verduras', 'Vegetables', 20, 3.20, 0.25, 0.14, 2.70, 100, 1, 'CIQUAL', '20027', 1),
    ('Kale', 'Kale', 'Verduras', 'Vegetables', 35, 2.92, 0.32, 1.49, 4.10, 100, 1, 'CIQUAL', '20346', 1),
    ('Brócoli, crudo', 'Broccoli, raw', 'Verduras', 'Vegetables', 32, 2.90, 2.15, 0.36, 2.60, 100, 1, 'CIQUAL', '20057', 1),
    ('Brócoli, cocido', 'Broccoli, cooked', 'Verduras', 'Vegetables', 30, 3.17, 1.79, 0.60, 2.59, 100, 1, 'CIQUAL', '20351', 1),
    ('Coliflor, cruda', 'Cauliflower, raw', 'Verduras', 'Vegetables', 25, 1.81, 2.13, 0.70, 2.20, 100, 1, 'CIQUAL', '20016', 1),
    ('Coliflor, cocida', 'Cauliflower, cooked', 'Verduras', 'Vegetables', 21, 1.60, 1.60, 0.46, 2.00, 100, 1, 'CIQUAL', '20017', 1),
    ('Coles de Bruselas', 'Brussels sprouts', 'Verduras', 'Vegetables', 44, 3.38, 4.59, 0.30, 3.80, 100, 1, 'CIQUAL', '20058', 1),
    ('Repollo', 'Green cabbage', 'Verduras', 'Vegetables', 24, 1.43, 2.74, 0.14, 2.55, 100, 1, 'CIQUAL', '20069', 1),
    ('Lombarda', 'Red cabbage', 'Verduras', 'Vegetables', 26, 1.13, 4.33, 0.25, 2.80, 100, 1, 'CIQUAL', '20014', 1),
    ('Zanahoria', 'Carrot', 'Verduras', 'Vegetables', 30, 0.78, 5.16, 0.25, 2.90, 100, 1, 'CIQUAL', '20009', 1),
    ('Zanahoria, cocida', 'Carrot, cooked', 'Verduras', 'Vegetables', 40, 0.63, 7.33, 0.30, 3.70, 100, 1, 'CIQUAL', '20307', 1),
    ('Calabacín', 'Courgette', 'Verduras', 'Vegetables', 17, 1.21, 1.75, 0.32, 1.00, 100, 1, 'CIQUAL', '20020', 1),
    ('Calabacín, cocinado', 'Courgette, cooked', 'Verduras', 'Vegetables', 16, 0.93, 1.40, 0.36, 1.50, 100, 1, 'CIQUAL', '20021', 1),
    ('Berenjena', 'Aubergine', 'Verduras', 'Vegetables', 23, 0.98, 2.70, 0.18, 3.00, 100, 1, 'CIQUAL', '20053', 1),
    ('Pimiento rojo', 'Red pepper', 'Verduras', 'Vegetables', 34, 1.06, 5.98, 0.25, 3.20, 100, 1, 'CIQUAL', '20087', 1),
    ('Pimiento verde', 'Green pepper', 'Verduras', 'Vegetables', 22, 0.81, 3.43, 0.25, 3.20, 100, 1, 'CIQUAL', '20085', 1),
    ('Pimiento asado en conserva', 'Canned roasted pepper', 'Verduras', 'Vegetables', 26, 0.96, 4.50, 0.20, 1.35, 100, 1, 'CIQUAL', '20275', 1),
    ('Cebolla', 'Onion', 'Verduras', 'Vegetables', 39, 1.10, 6.25, 0.62, 1.70, 100, 1, 'CIQUAL', '20034', 1),
    ('Cebolla morada', 'Red onion', 'Verduras', 'Vegetables', 35, 1.31, 5.63, 0.40, 2.50, 100, 1, 'CIQUAL', '20238', 1),
    ('Cebolla, cocinada', 'Onion, cooked', 'Verduras', 'Vegetables', 35, 1.30, 6.20, 0.20, 1.40, 100, 1, 'CIQUAL', '20035', 1),
    ('Ajo', 'Garlic', 'Verduras', 'Vegetables', 109, 5.31, 18.60, 0.25, 5.80, 100, 1, 'CIQUAL', '11000', 1),
    ('Puerro', 'Leek', 'Verduras', 'Vegetables', 30, 1.50, 4.55, 0.30, 1.80, 100, 1, 'CIQUAL', '20039', 1),
    ('Apio', 'Celery', 'Verduras', 'Vegetables', 15, 0.63, 2.41, 0.25, 2.20, 100, 1, 'CIQUAL', '20023', 1),
    ('Champiñones', 'Mushrooms', 'Verduras', 'Vegetables', 21, 2.11, 1.83, 0.36, 1.00, 100, 1, 'CIQUAL', '20056', 1),
    ('Champiñones salteados', 'Sautéed mushrooms', 'Verduras', 'Vegetables', 38, 4.44, 4.53, 0.60, 0.90, 100, 1, 'CIQUAL', '20125', 1),
    ('Setas', 'Oyster mushrooms', 'Verduras', 'Vegetables', 27, 3.31, 1.50, 0.41, 2.30, 100, 1, 'CIQUAL', '20114', 1),
    ('Judías verdes', 'Green beans', 'Verduras', 'Vegetables', 32, 1.83, 4.14, 0.22, 2.70, 100, 1, 'CIQUAL', '20061', 1),
    ('Judías verdes, cocidas', 'Green beans, cooked', 'Verduras', 'Vegetables', 29, 2.00, 3.00, 0.17, 4.00, 100, 1, 'CIQUAL', '20030', 1),
    ('Espárragos verdes', 'Green asparagus', 'Verduras', 'Vegetables', 25, 2.46, 2.03, 0.27, 2.15, 100, 1, 'CIQUAL', '20279', 1),
    ('Espárragos blancos en conserva', 'Canned white asparagus', 'Verduras', 'Vegetables', 20, 1.57, 1.20, 0.65, 1.55, 100, 1, 'CIQUAL', '20076', 1),
    ('Alcachofa', 'Artichoke', 'Verduras', 'Vegetables', 45, 3.27, 4.84, 0.15, 5.40, 100, 1, 'CIQUAL', '20052', 1),
    ('Alcachofa, cocida', 'Artichoke, cooked', 'Verduras', 'Vegetables', 33, 2.53, 0.99, 0.28, 8.30, 100, 1, 'CIQUAL', '20000', 1),
    ('Remolacha, cruda', 'Beetroot, raw', 'Verduras', 'Vegetables', 40, 1.61, 6.34, 0.17, 2.80, 100, 1, 'CIQUAL', '20091', 1),
    ('Remolacha, cocida', 'Beetroot, cooked', 'Verduras', 'Vegetables', 42, 1.44, 7.13, 0.40, 2.50, 100, 1, 'CIQUAL', '20003', 1),
    ('Rábanos', 'Radishes', 'Verduras', 'Vegetables', 11, 0.94, 1.53, 0.25, 1.40, 100, 1, 'CIQUAL', '20045', 1),
    ('Calabaza', 'Pumpkin', 'Verduras', 'Vegetables', 20, 1.05, 3.50, 0.10, 0.50, 100, 1, 'CIQUAL', '20044', 1),
    ('Calabaza butternut', 'Butternut squash', 'Verduras', 'Vegetables', 30, 1.00, 5.40, 0.10, 2.00, 100, 1, 'CIQUAL', '20138', 1),
    ('Endibia', 'Chicory', 'Verduras', 'Vegetables', 17, 1.19, 2.83, 0.25, 1.10, 100, 1, 'CIQUAL', '20026', 1),
    ('Acelgas', 'Swiss chard', 'Verduras', 'Vegetables', 13, 1.00, 1.63, 0.25, 1.80, 100, 1, 'CIQUAL', '20004', 1),
    ('Hinojo', 'Fennel', 'Verduras', 'Vegetables', 18, 1.00, 2.63, 0.25, 2.60, 100, 1, 'CIQUAL', '20028', 1),
    ('Nabo', 'Turnip', 'Verduras', 'Vegetables', 26, 0.90, 4.39, 0.10, 1.80, 100, 1, 'CIQUAL', '20064', 1),
    ('Maíz dulce en conserva', 'Canned sweet corn', 'Verduras', 'Vegetables', 105, 2.66, 18.30, 1.68, 3.10, 100, 1, 'CIQUAL', '20066', 1),
    ('Brotes de soja', 'Bean sprouts', 'Verduras', 'Vegetables', 28, 2.81, 3.40, 0.25, 1.70, 100, 1, 'CIQUAL', '20183', 1),
    ('Perejil', 'Parsley', 'Verduras', 'Vegetables', 42, 2.97, 4.10, 0.63, 4.30, 100, 1, 'CIQUAL', '11014', 1),
    ('Pepinillos en vinagre', 'Pickled gherkins', 'Verduras', 'Vegetables', 16, 1.06, 0.78, 0.30, 1.50, 100, 1, 'CIQUAL', '11004', 1),
    ('Gazpacho', 'Gazpacho', 'Verduras', 'Vegetables', 40, 0.81, 3.21, 2.33, 1.07, 100, 1, 'CIQUAL', '25967', 1),
    ('Almendras', 'Almonds', 'Frutos secos', 'Nuts', 615, 22.60, 9.51, 51.30, 12.50, 100, 1, 'CIQUAL', '15000', 1),
    ('Almendras tostadas saladas', 'Salted roasted almonds', 'Frutos secos', 'Nuts', 647, 24.10, 12.20, 53.90, 8.46, 100, 1, 'CIQUAL', '15042', 1),
    ('Nueces', 'Walnuts', 'Frutos secos', 'Nuts', 709, 15.70, 6.88, 67.30, 6.70, 100, 1, 'CIQUAL', '15005', 1),
    ('Avellanas', 'Hazelnuts', 'Frutos secos', 'Nuts', 632, 17.00, 7.16, 56.90, 11.60, 100, 1, 'CIQUAL', '15004', 1),
    ('Anacardos', 'Cashews', 'Frutos secos', 'Nuts', 618, 20.50, 21.30, 48.10, 8.40, 100, 1, 'CIQUAL', '15054', 1),
    ('Pistachos', 'Pistachios', 'Frutos secos', 'Nuts', 631, 21.70, 14.90, 51.60, 10.20, 100, 1, 'CIQUAL', '15044', 1),
    ('Cacahuetes', 'Peanuts', 'Frutos secos', 'Nuts', 623, 26.10, 14.80, 49.10, 8.60, 100, 1, 'CIQUAL', '15001', 1),
    ('Cacahuetes tostados salados', 'Salted roasted peanuts', 'Frutos secos', 'Nuts', 635, 26.20, 14.20, 50.80, 8.08, 100, 1, 'CIQUAL', '15002', 1),
    ('Nueces de Brasil', 'Brazil nuts', 'Frutos secos', 'Nuts', 701, 16.40, 4.23, 67.10, 7.50, 100, 1, 'CIQUAL', '15008', 1),
    ('Nueces de macadamia', 'Macadamia nuts', 'Frutos secos', 'Nuts', 757, 9.33, 5.22, 75.80, 8.60, 100, 1, 'CIQUAL', '15027', 1),
    ('Piñones', 'Pine nuts', 'Frutos secos', 'Nuts', 712, 16.20, 2.93, 68.40, 10.00, 100, 1, 'CIQUAL', '15025', 1),
    ('Frutos secos variados', 'Mixed nuts', 'Frutos secos', 'Nuts', 632, 18.20, 10.50, 55.00, 10.70, 100, 1, 'CIQUAL', '15066', 1),
    ('Pipas de girasol', 'Sunflower seeds', 'Frutos secos', 'Nuts', 653, 25.10, 10.10, 55.50, 6.40, 100, 1, 'CIQUAL', '15011', 1),
    ('Pipas de calabaza', 'Pumpkin seeds', 'Frutos secos', 'Nuts', 618, 35.60, 5.39, 49.10, 6.00, 100, 1, 'CIQUAL', '15064', 1),
    ('Semillas de chía', 'Chia seeds', 'Frutos secos', 'Nuts', 454, 19.50, 7.72, 30.70, 34.40, 100, 1, 'CIQUAL', '15047', 1),
    ('Semillas de lino', 'Flaxseed', 'Frutos secos', 'Nuts', 528, 22.40, 6.49, 40.30, 24.40, 100, 1, 'CIQUAL', '15034', 1),
    ('Sésamo', 'Sesame seeds', 'Frutos secos', 'Nuts', 606, 20.90, 12.80, 49.70, 11.80, 100, 1, 'CIQUAL', '15010', 1),
    ('Crema de cacahuete', 'Peanut butter', 'Frutos secos', 'Nuts', 643, 25.40, 17.30, 51.40, 5.00, 100, 1, 'CIQUAL', '15202', 1),
    ('Coco rallado', 'Desiccated coconut', 'Frutos secos', 'Nuts', 692, 8.06, 7.81, 66.80, 13.70, 100, 1, 'CIQUAL', '15007', 1),
    ('Aceite de oliva virgen extra', 'Extra virgin olive oil', 'Aceites y grasas', 'Fats', 899, 0.25, 0.00, 99.90, 0.00, 100, 1, 'CIQUAL', '17270', 1),
    ('Aceite de girasol', 'Sunflower oil', 'Aceites y grasas', 'Fats', 900, 0.25, 0.00, 100.00, 0.00, 100, 1, 'CIQUAL', '17440', 1),
    ('Aceite de colza', 'Rapeseed oil', 'Aceites y grasas', 'Fats', 900, 0.00, 0.00, 100.00, 0.00, 100, 1, 'CIQUAL', '17130', 1),
    ('Aceite de coco', 'Coconut oil', 'Aceites y grasas', 'Fats', 899, 0.16, 0.00, 99.90, 0.00, 100, 1, 'CIQUAL', '16050', 1),
    ('Manteca de cerdo', 'Lard', 'Aceites y grasas', 'Fats', 900, 0.00, 0.00, 100.00, 0.00, 100, 1, 'CIQUAL', '16520', 1),
    ('Mayonesa', 'Mayonnaise', 'Aceites y grasas', 'Fats', 692, 1.33, 3.41, 74.50, 0.26, 100, 1, 'CIQUAL', '11054', 1),
    ('Mayonesa light', 'Light mayonnaise', 'Aceites y grasas', 'Fats', 297, 1.09, 8.82, 28.50, 0.31, 100, 1, 'CIQUAL', '11079', 1),
    ('Alioli', 'Aioli', 'Aceites y grasas', 'Fats', 480, 1.13, 5.44, 50.20, 0.25, 100, 1, 'CIQUAL', '11168', 1),
    ('Vinagreta de aceite de oliva', 'Olive oil vinaigrette', 'Aceites y grasas', 'Fats', 471, 0.70, 3.03, 50.50, 0.53, 100, 1, 'CIQUAL', '11108', 1),
    ('Agua', 'Water', 'Bebidas', 'Drinks', 0, 0.00, 0.01, 0.00, 0.00, 100, 1, 'CIQUAL', '18045', 1),
    ('Café solo', 'Black coffee', 'Bebidas', 'Drinks', 6, 0.25, 1.35, 0.02, 0.25, 100, 1, 'CIQUAL', '18004', 1),
    ('Café con leche sin azúcar', 'Coffee with milk, unsweetened', 'Bebidas', 'Drinks', 22, 1.36, 2.31, 0.76, 0.00, 100, 1, 'CIQUAL', '18151', 1),
    ('Té negro', 'Black tea', 'Bebidas', 'Drinks', 1, 0.00, 0.30, 0.01, 0.00, 100, 1, 'CIQUAL', '18154', 1),
    ('Té verde', 'Green tea', 'Bebidas', 'Drinks', 5, 0.00, 1.09, 0.00, 0.00, 100, 1, 'CIQUAL', '18155', 1),
    ('Infusión', 'Herbal tea', 'Bebidas', 'Drinks', 1, 0.00, 0.20, 0.00, 0.00, 100, 1, 'CIQUAL', '18022', 1),
    ('Zumo de naranja natural', 'Fresh orange juice', 'Bebidas', 'Drinks', 45, 0.70, 9.19, 0.20, 0.20, 100, 1, 'CIQUAL', '2013', 1),
    ('Zumo de naranja envasado', 'Bottled orange juice', 'Bebidas', 'Drinks', 45, 0.61, 9.61, 0.11, 0.28, 100, 1, 'CIQUAL', '2070', 1),
    ('Zumo de manzana', 'Apple juice', 'Bebidas', 'Drinks', 49, 0.17, 11.40, 0.14, 0.23, 100, 1, 'CIQUAL', '2074', 1),
    ('Zumo de uva', 'Grape juice', 'Bebidas', 'Drinks', 69, 0.25, 16.30, 0.06, 0.14, 100, 1, 'CIQUAL', '2016', 1),
    ('Refresco de cola', 'Cola', 'Bebidas', 'Drinks', 40, 0.00, 10.00, 0.00, 0.00, 100, 1, 'CIQUAL', '18018', 1),
    ('Gaseosa', 'Lemonade soda', 'Bebidas', 'Drinks', 33, 0.00, 8.24, 0.00, 0.14, 100, 1, 'CIQUAL', '18010', 1),
    ('Bebida isotónica', 'Sports drink', 'Bebidas', 'Drinks', 22, 0.00, 5.41, 0.00, 0.00, 100, 1, 'CIQUAL', '18348', 1),
    ('Bebida energética', 'Energy drink', 'Bebidas', 'Drinks', 41, 0.10, 10.20, 0.00, 0.11, 100, 1, 'CIQUAL', '18352', 1),
    ('Kombucha', 'Kombucha', 'Bebidas', 'Drinks', 7, 0.25, 1.45, 0.15, 0.25, 100, 1, 'CIQUAL', '18025', 1),
    ('Bebida de soja', 'Soy drink', 'Bebidas', 'Drinks', 42, 3.50, 1.87, 2.10, 0.71, 100, 1, 'CIQUAL', '18900', 1),
    ('Bebida de avena', 'Oat drink', 'Bebidas', 'Drinks', 41, 0.25, 7.78, 1.10, 0.25, 100, 1, 'CIQUAL', '18899', 1),
    ('Bebida de almendra', 'Almond drink', 'Bebidas', 'Drinks', 36, 1.06, 0.68, 3.20, 0.25, 100, 1, 'CIQUAL', '18107', 1),
    ('Bebida de arroz', 'Rice drink', 'Bebidas', 'Drinks', 52, 0.25, 10.80, 1.00, 0.25, 100, 1, 'CIQUAL', '18904', 1),
    ('Cacao soluble con leche', 'Hot chocolate with milk', 'Bebidas', 'Drinks', 66, 3.78, 9.50, 1.42, 0.00, 100, 1, 'CIQUAL', '18104', 1),
    ('Cerveza', 'Beer', 'Bebidas', 'Drinks', 39, 0.39, 3.12, 0.00, 0.00, 100, 1, 'CIQUAL', '5001', 1),
    ('Vino tinto', 'Red wine', 'Bebidas', 'Drinks', 76, 0.16, 0.00, 0.25, 0.50, 100, 1, 'CIQUAL', '5214', 1),
    ('Vino blanco', 'White wine', 'Bebidas', 'Drinks', 55, 0.25, 0.17, 0.00, 0.00, 100, 1, 'CIQUAL', '5215', 1),
    ('Vino rosado', 'Rosé wine', 'Bebidas', 'Drinks', 69, 0.15, 1.40, 0.00, 0.00, 100, 1, 'CIQUAL', '5216', 1),
    ('Cava', 'Sparkling wine', 'Bebidas', 'Drinks', 72, 0.10, 1.78, 0.00, 0.00, 100, 1, 'CIQUAL', '5201', 1),
    ('Sidra', 'Cider', 'Bebidas', 'Drinks', 36, 0.07, 3.67, 0.09, 0.00, 100, 1, 'CIQUAL', '5003', 1),
    ('Sangría', 'Sangria', 'Bebidas', 'Drinks', 89, 0.00, 11.00, 0.05, 0.00, 100, 1, 'CIQUAL', '1017', 1),
    ('Vermut', 'Vermouth', 'Bebidas', 'Drinks', 137, 0.05, 11.30, 0.00, 0.00, 100, 1, 'CIQUAL', '1007', 1),
    ('Whisky', 'Whisky', 'Bebidas', 'Drinks', 252, 0.00, 0.10, 0.00, 0.00, 100, 1, 'CIQUAL', '1005', 1),
    ('Ron', 'Rum', 'Bebidas', 'Drinks', 234, 0.00, 0.00, 0.00, 0.00, 100, 1, 'CIQUAL', '1004', 1),
    ('Ginebra', 'Gin', 'Bebidas', 'Drinks', 265, 0.00, 0.00, 0.00, 0.00, 100, 1, 'CIQUAL', '1002', 1),
    ('Vodka', 'Vodka', 'Bebidas', 'Drinks', 250, 0.00, 0.00, 0.00, 0.00, 100, 1, 'CIQUAL', '1008', 1),
    ('Proteína de suero en polvo', 'Whey protein powder', 'Suplementos', 'Supplements', 352, 78.13, 6.25, 1.56, 3.10, 100, 1, 'USDA', '173180', 1),
    ('Proteína de suero aislada en polvo', 'Whey protein isolate powder', 'Suplementos', 'Supplements', 359, 58.14, 29.07, 1.16, 0.00, 100, 1, 'USDA', '173177', 1),
    ('Proteína de soja aislada', 'Soy protein isolate', 'Suplementos', 'Supplements', 335, 88.32, 0.00, 3.39, 0.00, 100, 1, 'USDA', '174276', 1),
    ('Gelatina en polvo', 'Gelatin powder', 'Suplementos', 'Supplements', 348, 86.90, 0.00, 0.10, 0.00, 100, 1, 'CIQUAL', '11007', 1),
    ('Clara de huevo en polvo', 'Egg white powder', 'Suplementos', 'Supplements', 356, 81.10, 7.80, 0.00, 0.00, 100, 1, 'CIQUAL', '22004', 1),
    ('Levadura de cerveza', 'Brewer''s yeast', 'Suplementos', 'Supplements', 334, 40.40, 21.80, 4.50, 22.50, 100, 1, 'CIQUAL', '11009', 1),
    ('Germen de trigo', 'Wheat germ', 'Suplementos', 'Supplements', 375, 29.20, 35.10, 9.50, 16.30, 100, 1, 'CIQUAL', '9660', 1),
    ('Barrita sustitutiva', 'Meal replacement bar', 'Suplementos', 'Supplements', 391, 5.90, 71.00, 11.20, 5.80, 100, 1, 'CIQUAL', '31104', 1),
    ('Patatas fritas de bolsa', 'Potato crisps', 'Snacks', 'Snacks', 532, 6.00, 51.00, 32.70, 4.70, 100, 1, 'CIQUAL', '4004', 1),
    ('Nachos', 'Tortilla chips', 'Snacks', 'Snacks', 488, 6.68, 62.60, 22.50, 4.33, 100, 1, 'CIQUAL', '38417', 1),
    ('Palomitas', 'Popcorn', 'Snacks', 'Snacks', 419, 11.00, 59.70, 12.30, 12.80, 100, 1, 'CIQUAL', '9231', 1),
    ('Palomitas saladas con aceite', 'Salted popcorn with oil', 'Snacks', 'Snacks', 477, 8.30, 50.20, 24.60, 10.60, 100, 1, 'CIQUAL', '9230', 1),
    ('Galletas María', 'Plain biscuits', 'Snacks', 'Snacks', 438, 7.13, 74.90, 11.50, 3.30, 100, 1, 'CIQUAL', '24001', 1),
    ('Galletas integrales', 'Wholegrain biscuits', 'Snacks', 'Snacks', 462, 7.75, 63.00, 18.40, 6.60, 100, 1, 'CIQUAL', '24020', 1),
    ('Galletas de chocolate', 'Chocolate biscuits', 'Snacks', 'Snacks', 507, 7.75, 59.50, 25.60, 3.90, 100, 1, 'CIQUAL', '24036', 1),
    ('Cookies', 'Chocolate chip cookies', 'Snacks', 'Snacks', 493, 6.44, 61.00, 23.80, 4.30, 100, 1, 'CIQUAL', '24684', 1),
    ('Magdalenas', 'Madeleine cakes', 'Snacks', 'Snacks', 441, 5.55, 55.30, 21.60, 1.40, 100, 1, 'CIQUAL', '24632', 1),
    ('Bizcocho', 'Sponge cake', 'Snacks', 'Snacks', 438, 5.68, 50.50, 23.20, 1.83, 100, 1, 'CIQUAL', '23925', 1),
    ('Pastel de chocolate', 'Chocolate cake', 'Snacks', 'Snacks', 445, 5.88, 51.00, 24.00, 3.70, 100, 1, 'CIQUAL', '23585', 1),
    ('Donut', 'Doughnut', 'Snacks', 'Snacks', 404, 7.25, 39.40, 23.70, 2.00, 100, 1, 'CIQUAL', '23880', 1),
    ('Crepe', 'Crepe', 'Snacks', 'Snacks', 257, 6.31, 41.60, 6.80, 2.20, 100, 1, 'CIQUAL', '23799', 1),
    ('Gofre', 'Waffle', 'Snacks', 'Snacks', 301, 8.00, 36.10, 13.40, 1.90, 100, 1, 'CIQUAL', '23850', 1),
    ('Barrita de cereales con chocolate', 'Chocolate cereal bar', 'Snacks', 'Snacks', 451, 6.55, 73.00, 14.90, 4.50, 100, 1, 'CIQUAL', '31106', 1),
    ('Barrita de cereales con frutas', 'Fruit cereal bar', 'Snacks', 'Snacks', 379, 5.49, 74.10, 7.07, 3.80, 100, 1, 'CIQUAL', '31113', 1),
    ('Chocolate con leche', 'Milk chocolate', 'Snacks', 'Snacks', 550, 7.50, 58.90, 30.80, 2.90, 100, 1, 'CIQUAL', '31004', 1),
    ('Chocolate negro', 'Dark chocolate', 'Snacks', 'Snacks', 521, 5.98, 53.00, 29.60, 8.31, 100, 1, 'CIQUAL', '31005', 1),
    ('Chocolate blanco', 'White chocolate', 'Snacks', 'Snacks', 563, 6.16, 57.10, 34.20, 1.00, 100, 1, 'CIQUAL', '31010', 1),
    ('Crema de cacao y avellanas', 'Chocolate hazelnut spread', 'Snacks', 'Snacks', 549, 5.02, 57.90, 32.40, 3.23, 100, 1, 'CIQUAL', '31032', 1),
    ('Barrita de chocolate', 'Chocolate bar', 'Snacks', 'Snacks', 492, 5.56, 61.40, 24.10, 3.70, 100, 1, 'CIQUAL', '31000', 1),
    ('Gominolas', 'Gummy sweets', 'Snacks', 'Snacks', 339, 4.88, 76.90, 0.53, 1.50, 100, 1, 'CIQUAL', '31060', 1),
    ('Helado', 'Ice cream', 'Snacks', 'Snacks', 191, 2.50, 26.10, 8.40, 1.50, 100, 1, 'CIQUAL', '39515', 1),
    ('Sorbete', 'Sorbet', 'Snacks', 'Snacks', 122, 0.40, 28.70, 0.56, 0.99, 100, 1, 'CIQUAL', '39524', 1),
    ('Polo de hielo', 'Ice lolly', 'Snacks', 'Snacks', 84, 0.17, 20.50, 0.17, 0.26, 100, 1, 'CIQUAL', '39526', 1),
    ('Azúcar', 'Sugar', 'Otro', 'Other', 399, 0.00, 99.70, 0.00, 0.00, 100, 1, 'CIQUAL', '31016', 1),
    ('Azúcar moreno', 'Brown sugar', 'Otro', 'Other', 393, 0.12, 98.10, 0.00, 0.00, 100, 1, 'CIQUAL', '31017', 1),
    ('Miel', 'Honey', 'Otro', 'Other', 331, 0.65, 82.10, 0.00, 0.00, 100, 1, 'CIQUAL', '31008', 1),
    ('Sirope de arce', 'Maple syrup', 'Otro', 'Other', 269, 0.04, 67.20, 0.06, 0.00, 100, 1, 'CIQUAL', '31034', 1),
    ('Mermelada de fresa', 'Strawberry jam', 'Otro', 'Other', 248, 0.25, 60.50, 0.50, 0.70, 100, 1, 'CIQUAL', '31024', 1),
    ('Mermelada light', 'Reduced-sugar jam', 'Otro', 'Other', 166, 0.31, 40.30, 0.10, 1.37, 100, 1, 'CIQUAL', '30995', 1),
    ('Cacao puro en polvo', 'Unsweetened cocoa powder', 'Otro', 'Other', 387, 22.40, 11.60, 20.60, 29.50, 100, 1, 'CIQUAL', '18100', 1),
    ('Kétchup', 'Ketchup', 'Otro', 'Other', 108, 1.23, 23.70, 0.16, 1.77, 100, 1, 'CIQUAL', '11008', 1),
    ('Mostaza', 'Mustard', 'Otro', 'Other', 151, 6.92, 4.33, 11.20, 0.50, 100, 1, 'CIQUAL', '11013', 1),
    ('Salsa de soja', 'Soy sauce', 'Otro', 'Other', 40, 7.25, 1.72, 0.25, 0.90, 100, 1, 'CIQUAL', '11104', 1),
    ('Vinagre', 'Vinegar', 'Otro', 'Other', 22, 0.04, 0.95, 0.10, 0.00, 100, 1, 'CIQUAL', '11018', 1),
    ('Vinagre balsámico', 'Balsamic vinegar', 'Otro', 'Other', 92, 0.69, 18.00, 0.30, 0.75, 100, 1, 'CIQUAL', '11091', 1),
    ('Pesto', 'Pesto', 'Otro', 'Other', 370, 3.94, 6.60, 35.40, 2.50, 100, 1, 'CIQUAL', '11179', 1),
    ('Salsa de tomate', 'Tomato sauce', 'Otro', 'Other', 42, 2.04, 4.71, 0.75, 2.81, 100, 1, 'CIQUAL', '25525', 1),
    ('Bechamel', 'Béchamel sauce', 'Otro', 'Other', 147, 3.84, 8.97, 10.60, 0.20, 100, 1, 'CIQUAL', '11143', 1),
    ('Guacamole', 'Guacamole', 'Otro', 'Other', 191, 1.82, 4.64, 17.20, 4.84, 100, 1, 'CIQUAL', '25620', 1),
    ('Caldo de pollo', 'Chicken broth', 'Otro', 'Other', 7, 1.23, 0.28, 0.13, 0.00, 100, 1, 'CIQUAL', '25947', 1),
    ('Paella', 'Paella', 'Otro', 'Other', 148, 7.86, 17.10, 4.90, 1.85, 100, 1, 'CIQUAL', '25031', 1),
    ('Pizza margarita', 'Margherita pizza', 'Otro', 'Other', 224, 8.70, 29.60, 7.41, 2.27, 100, 1, 'CIQUAL', '25404', 1),
    ('Pizza de jamón y queso', 'Ham and cheese pizza', 'Otro', 'Other', 228, 10.70, 26.20, 8.49, 1.95, 100, 1, 'CIQUAL', '25435', 1),
    ('Lasaña de carne', 'Meat lasagne', 'Otro', 'Other', 134, 6.29, 13.50, 5.72, 1.91, 100, 1, 'CIQUAL', '25081', 1),
    ('Espaguetis a la boloñesa', 'Spaghetti bolognese', 'Otro', 'Other', 116, 5.32, 13.60, 4.07, 1.90, 100, 1, 'CIQUAL', '25085', 1),
    ('Pasta a la carbonara', 'Pasta carbonara', 'Otro', 'Other', 161, 5.91, 14.20, 8.70, 1.18, 100, 1, 'CIQUAL', '25135', 1),
    ('Hamburguesa completa', 'Hamburger', 'Otro', 'Other', 251, 13.30, 29.50, 8.44, 1.80, 100, 1, 'CIQUAL', '25413', 1),
    ('Kebab', 'Kebab', 'Otro', 'Other', 272, 15.00, 29.90, 9.80, 2.00, 100, 1, 'CIQUAL', '25429', 1),
    ('Bocadillo', 'Sandwich', 'Otro', 'Other', 280, 12.40, 30.30, 11.70, 2.08, 100, 1, 'CIQUAL', '25522', 1),
    ('Croquetas', 'Croquettes', 'Otro', 'Other', 164, 2.52, 22.40, 6.59, 2.43, 100, 1, 'CIQUAL', '4049', 1),
    ('Falafel', 'Falafel', 'Otro', 'Other', 211, 7.38, 17.70, 10.80, 6.00, 100, 1, 'CIQUAL', '25590', 1),
    ('Cuscús con pollo', 'Chicken couscous', 'Otro', 'Other', 130, 7.50, 15.60, 3.85, 1.47, 100, 1, 'CIQUAL', '25138', 1),
    ('Sal', 'Salt', 'Otro', 'Other', 0, 0.00, 0.00, 0.00, 0.00, 100, 1, 'CIQUAL', '11058', 1)
ON DUPLICATE KEY UPDATE id = id;

INSERT INTO alimento_raciones (alimento_id, nombre, nombre_en, gramos, fuente, orden)
SELECT a.id, r.nombre, r.nombre_en, r.gramos, r.fuente_peso, r.orden
FROM alimentos a
JOIN (
    SELECT 'CIQUAL' AS f, '36017' AS c, '1 pechuga entera' AS nombre, '1 whole breast' AS nombre_en, 272.0 AS gramos, 'USDA FDC 171077: 1 piece' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '36017' AS c, '1 filete' AS nombre, '1 fillet' AS nombre_en, 113.0 AS gramos, 'USDA FDC 171077: 4 oz' AS fuente_peso, 2 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '36018' AS c, '1 filete' AS nombre, '1 fillet' AS nombre_en, 113.0 AS gramos, 'USDA FDC 171077: 4 oz' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '36024' AS c, '1 muslo sin hueso' AS nombre, '1 boneless thigh' AS nombre_en, 52.0 AS gramos, 'USDA FDC 172387: 1 thigh, bone and skin removed' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '36006' AS c, '1 muslo sin hueso' AS nombre, '1 boneless thigh' AS nombre_en, 52.0 AS gramos, 'USDA FDC 172387: 1 thigh, bone and skin removed' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '36002' AS c, '1 muslo con piel' AS nombre, '1 thigh with skin' AS nombre_en, 193.0 AS gramos, 'USDA FDC 172385: 1 thigh with skin' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '36023' AS c, '1 alita' AS nombre, '1 wing' AS nombre_en, 107.0 AS gramos, 'USDA FDC 172390: 1 piece' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '36005' AS c, '1 ración' AS nombre, '1 serving' AS nombre_en, 85.0 AS gramos, 'USDA FDC 171474: 3 oz' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '36304' AS c, '1 filete' AS nombre, '1 fillet' AS nombre_en, 113.0 AS gramos, 'USDA FDC 171098: 4 oz' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '36306' AS c, '1 filete' AS nombre, '1 fillet' AS nombre_en, 113.0 AS gramos, 'USDA FDC 171098: 4 oz' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '36307' AS c, '1 ración' AS nombre, '1 serving' AS nombre_en, 113.0 AS gramos, 'USDA FDC 171098: 4 oz' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '6201' AS c, '1 filete' AS nombre, '1 steak' AS nombre_en, 113.0 AS gramos, 'USDA FDC 174762: 4 oz' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '6206' AS c, '1 filete' AS nombre, '1 steak' AS nombre_en, 199.0 AS gramos, 'USDA FDC 173409: 1 steak' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '6207' AS c, '1 filete' AS nombre, '1 steak' AS nombre_en, 199.0 AS gramos, 'USDA FDC 173409: 1 steak' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '6116' AS c, '1 filete' AS nombre, '1 steak' AS nombre_en, 113.0 AS gramos, 'USDA FDC 174762: 4 oz' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '6103' AS c, '1 entrecot' AS nombre, '1 steak' AS nombre_en, 238.0 AS gramos, 'USDA FDC 172174: 1 steak' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '6250' AS c, '1 hamburguesa' AS nombre, '1 patty' AS nombre_en, 113.0 AS gramos, 'USDA FDC 174030: 4 oz' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '6252' AS c, '1 hamburguesa' AS nombre, '1 patty' AS nombre_en, 113.0 AS gramos, 'USDA FDC 174030: 4 oz' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '6256' AS c, '1 hamburguesa' AS nombre, '1 patty' AS nombre_en, 113.0 AS gramos, 'USDA FDC 174030: 4 oz' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '6253' AS c, '1 ración' AS nombre, '1 serving' AS nombre_en, 85.0 AS gramos, 'USDA FDC 174030: 3 oz' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '6231' AS c, '1 ración' AS nombre, '1 serving' AS nombre_en, 113.0 AS gramos, 'USDA FDC 174762: 4 oz' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '6521' AS c, '1 filete' AS nombre, '1 escalope' AS nombre_en, 113.0 AS gramos, 'USDA FDC 174762: 4 oz' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '28204' AS c, '1 solomillo entero' AS nombre, '1 whole tenderloin' AS nombre_en, 505.0 AS gramos, 'USDA FDC 168249: 1 roast' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '28204' AS c, '1 ración' AS nombre, '1 serving' AS nombre_en, 113.0 AS gramos, 'USDA FDC 168249: 4 oz' AS fuente_peso, 2 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '28203' AS c, '1 ración' AS nombre, '1 serving' AS nombre_en, 85.0 AS gramos, 'USDA FDC 168249: 3 oz' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '28100' AS c, '1 chuleta sin hueso' AS nombre, '1 boneless chop' AS nombre_en, 178.0 AS gramos, 'USDA FDC 167829: 1 chop without refuse' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '28101' AS c, '1 chuleta sin hueso' AS nombre, '1 boneless chop' AS nombre_en, 178.0 AS gramos, 'USDA FDC 167829: 1 chop without refuse' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '28302' AS c, '1 ración' AS nombre, '1 serving' AS nombre_en, 113.0 AS gramos, 'USDA FDC 167829: 4 oz' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '28002' AS c, '1 ración' AS nombre, '1 serving' AS nombre_en, 113.0 AS gramos, 'USDA FDC 167829: 4 oz' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '28472' AS c, '1 ración' AS nombre, '1 serving' AS nombre_en, 113.0 AS gramos, 'USDA FDC 174030: 4 oz' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '21521' AS c, '1 ración' AS nombre, '1 serving' AS nombre_en, 113.0 AS gramos, 'USDA FDC 174762: 4 oz' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '21502' AS c, '1 ración' AS nombre, '1 serving' AS nombre_en, 113.0 AS gramos, 'USDA FDC 174762: 4 oz' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '34001' AS c, '1 ración' AS nombre, '1 serving' AS nombre_en, 113.0 AS gramos, 'USDA FDC 174762: 4 oz' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '34002' AS c, '1 ración' AS nombre, '1 serving' AS nombre_en, 85.0 AS gramos, 'USDA FDC 174762: 3 oz' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '36206' AS c, '1 ración' AS nombre, '1 serving' AS nombre_en, 113.0 AS gramos, 'USDA FDC 174762: 4 oz' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '36101' AS c, '1 ración' AS nombre, '1 serving' AS nombre_en, 85.0 AS gramos, 'USDA FDC 174762: 3 oz' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '6900' AS c, '1 filete' AS nombre, '1 steak' AS nombre_en, 113.0 AS gramos, 'USDA FDC 174762: 4 oz' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '40108' AS c, '1 ración' AS nombre, '1 serving' AS nombre_en, 85.0 AS gramos, 'USDA FDC 174762: 3 oz' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '40106' AS c, '1 ración' AS nombre, '1 serving' AS nombre_en, 85.0 AS gramos, 'USDA FDC 174762: 3 oz' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '28845' AS c, '1 loncha' AS nombre, '1 slice' AS nombre_en, 15.0 AS gramos, 'USDA FDC 172941: 1 slice (loncha fina de embutido)' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '28845' AS c, '1 ración' AS nombre, '1 serving' AS nombre_en, 56.0 AS gramos, 'USDA FDC 173864: 56 grams 1 serving' AS fuente_peso, 2 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '28910' AS c, '1 loncha' AS nombre, '1 slice' AS nombre_en, 28.0 AS gramos, 'USDA FDC 173864: 1 slice' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '28900' AS c, '1 loncha' AS nombre, '1 slice' AS nombre_en, 28.0 AS gramos, 'USDA FDC 173864: 1 slice' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '28964' AS c, '1 loncha' AS nombre, '1 slice' AS nombre_en, 16.0 AS gramos, 'USDA FDC 172941: 1 slice' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '28963' AS c, '1 loncha' AS nombre, '1 slice' AS nombre_en, 16.0 AS gramos, 'USDA FDC 172941: 1 slice' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '30315' AS c, '1 loncha' AS nombre, '1 slice' AS nombre_en, 10.0 AS gramos, 'USDA FDC 172938: 1 slice' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '30315' AS c, '1 ración' AS nombre, '1 serving' AS nombre_en, 27.0 AS gramos, 'USDA FDC 174582: 3 slices 1 serving' AS fuente_peso, 2 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '30300' AS c, '1 loncha' AS nombre, '1 slice' AS nombre_en, 10.0 AS gramos, 'USDA FDC 172938: 1 slice' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '30300' AS c, '1 ración' AS nombre, '1 serving' AS nombre_en, 27.0 AS gramos, 'USDA FDC 174582: 3 slices 1 serving' AS fuente_peso, 2 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '30350' AS c, '1 loncha' AS nombre, '1 slice' AS nombre_en, 10.0 AS gramos, 'USDA FDC 172938: 1 slice' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '30789' AS c, '1 loncha' AS nombre, '1 slice' AS nombre_en, 28.0 AS gramos, 'USDA FDC 173864: 1 slice' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '8703' AS c, '1 ración' AS nombre, '1 serving' AS nombre_en, 85.0 AS gramos, 'USDA FDC 174762: 3 oz' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '30131' AS c, '1 salchicha' AS nombre, '1 sausage' AS nombre_en, 45.0 AS gramos, 'USDA FDC 171624: 1 link' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '30110' AS c, '1 salchicha' AS nombre, '1 sausage' AS nombre_en, 45.0 AS gramos, 'USDA FDC 171624: 1 link' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '28502' AS c, '1 loncha' AS nombre, '1 slice' AS nombre_en, 10.0 AS gramos, 'USDA FDC 172938: 1 slice' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '28727' AS c, '1 loncha' AS nombre, '1 slice' AS nombre_en, 10.0 AS gramos, 'USDA FDC 172938: 1 slice' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '8316' AS c, '1 ración' AS nombre, '1 serving' AS nombre_en, 28.0 AS gramos, 'USDA FDC 173864: 1 slice (1 oz)' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '36027' AS c, '5 nuggets' AS nombre, '5 nuggets' AS nombre_en, 88.0 AS gramos, 'USDA FDC 170795: 5 pieces' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '36318' AS c, '1 filete' AS nombre, '1 cutlet' AS nombre_en, 113.0 AS gramos, 'USDA FDC 171098: 4 oz' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '25163' AS c, '1 ración' AS nombre, '1 serving' AS nombre_en, 113.0 AS gramos, 'USDA FDC 174030: 4 oz' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '26036' AS c, '1 lomo' AS nombre, '1 fillet' AS nombre_en, 198.0 AS gramos, 'USDA FDC 175167: 0.5 fillet' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '26036' AS c, '1 ración' AS nombre, '1 serving' AS nombre_en, 85.0 AS gramos, 'USDA FDC 175167: 3 oz' AS fuente_peso, 2 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '26230' AS c, '1 ración' AS nombre, '1 serving' AS nombre_en, 85.0 AS gramos, 'USDA FDC 175167: 3 oz' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '26037' AS c, '1 ración' AS nombre, '1 serving' AS nombre_en, 28.0 AS gramos, 'USDA FDC 173709: 1 oz' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '26039' AS c, '1 lata pequeña escurrida' AS nombre, '1 small drained can' AS nombre_en, 56.0 AS gramos, 'Lata de 80 g, la más vendida en España: 56 g de peso escurrido declarado' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '26039' AS c, '1 ración' AS nombre, '1 serving' AS nombre_en, 85.0 AS gramos, 'USDA FDC 173709: 3 oz' AS fuente_peso, 2 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '26180' AS c, '1 lata pequeña escurrida' AS nombre, '1 small drained can' AS nombre_en, 56.0 AS gramos, 'Lata de 80 g, la más vendida en España: 56 g de peso escurrido declarado' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '26053' AS c, '1 ración' AS nombre, '1 serving' AS nombre_en, 85.0 AS gramos, 'USDA FDC 175159: 3 oz' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '26044' AS c, '1 filete' AS nombre, '1 fillet' AS nombre_en, 193.0 AS gramos, 'USDA FDC 171964: 1 fillet' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '26044' AS c, '1 ración' AS nombre, '1 serving' AS nombre_en, 85.0 AS gramos, 'USDA FDC 171964: 3 oz' AS fuente_peso, 2 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '26120' AS c, '1 ración' AS nombre, '1 serving' AS nombre_en, 85.0 AS gramos, 'USDA FDC 171964: 3 oz' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '26043' AS c, '1 filete' AS nombre, '1 fillet' AS nombre_en, 231.0 AS gramos, 'USDA FDC 171955: 1 fillet' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '26043' AS c, '1 ración' AS nombre, '1 serving' AS nombre_en, 85.0 AS gramos, 'USDA FDC 171955: 3 oz' AS fuente_peso, 2 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '26098' AS c, '1 ración' AS nombre, '1 serving' AS nombre_en, 28.0 AS gramos, 'USDA FDC 171955: 1 oz' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '26024' AS c, '1 ración' AS nombre, '1 serving' AS nombre_en, 85.0 AS gramos, 'USDA FDC 171955: 3 oz' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '26065' AS c, '1 sardina' AS nombre, '1 sardine' AS nombre_en, 12.0 AS gramos, 'USDA FDC 175139: 1 small' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '26040' AS c, '1 lata' AS nombre, '1 can' AS nombre_en, 92.0 AS gramos, 'USDA FDC 175139: 1 can (3.75 oz)' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '26040' AS c, '2 sardinas' AS nombre, '2 sardines' AS nombre_en, 24.0 AS gramos, 'USDA FDC 175139: 2 sardines' AS fuente_peso, 2 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '26051' AS c, '1 filete' AS nombre, '1 fillet' AS nombre_en, 112.0 AS gramos, 'USDA FDC 175119: 1 fillet' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '26079' AS c, '1 ración' AS nombre, '1 serving' AS nombre_en, 85.0 AS gramos, 'USDA FDC 175119: 3 oz' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '26694' AS c, '1 anchoa' AS nombre, '1 anchovy' AS nombre_en, 4.0 AS gramos, 'USDA FDC 174183: 1 anchovy' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '26694' AS c, '1 lata' AS nombre, '1 can' AS nombre_en, 45.0 AS gramos, 'USDA FDC 174183: 1 can (2 oz)' AS fuente_peso, 2 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '26088' AS c, '1 ración' AS nombre, '1 serving' AS nombre_en, 85.0 AS gramos, 'USDA FDC 171964: 3 oz' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '26134' AS c, '1 filete' AS nombre, '1 fillet' AS nombre_en, 193.0 AS gramos, 'USDA FDC 171964: 1 fillet' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '26006' AS c, '1 filete' AS nombre, '1 fillet' AS nombre_en, 193.0 AS gramos, 'USDA FDC 171964: 1 fillet' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '26058' AS c, '1 ración' AS nombre, '1 serving' AS nombre_en, 85.0 AS gramos, 'USDA FDC 171964: 3 oz' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '27009' AS c, '1 filete' AS nombre, '1 fillet' AS nombre_en, 79.0 AS gramos, 'USDA FDC 173717: 1 fillet' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '26018' AS c, '1 ración' AS nombre, '1 serving' AS nombre_en, 85.0 AS gramos, 'USDA FDC 171964: 3 oz' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '26174' AS c, '1 ración' AS nombre, '1 serving' AS nombre_en, 85.0 AS gramos, 'USDA FDC 171964: 3 oz' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '26082' AS c, '1 rodaja' AS nombre, '1 steak' AS nombre_en, 136.0 AS gramos, 'USDA FDC 173703: 1 piece' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '26011' AS c, '1 ración' AS nombre, '1 serving' AS nombre_en, 85.0 AS gramos, 'USDA FDC 175119: 3 oz' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '10021' AS c, '1 ración' AS nombre, '1 serving' AS nombre_en, 85.0 AS gramos, 'USDA FDC 175179: 3 oz' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '10007' AS c, '1 ración' AS nombre, '1 serving' AS nombre_en, 85.0 AS gramos, 'USDA FDC 175179: 3 oz' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '10024' AS c, '1 ración' AS nombre, '1 serving' AS nombre_en, 85.0 AS gramos, 'USDA FDC 175179: 3 oz' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '10014' AS c, '1 mejillón' AS nombre, '1 mussel' AS nombre_en, 16.0 AS gramos, 'USDA FDC 174216: 1 medium' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '10014' AS c, '1 ración' AS nombre, '1 serving' AS nombre_en, 85.0 AS gramos, 'USDA FDC 174216: 3 oz' AS fuente_peso, 2 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '10013' AS c, '1 ración' AS nombre, '1 serving' AS nombre_en, 85.0 AS gramos, 'USDA FDC 174216: 3 oz' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '10001' AS c, '1 ración' AS nombre, '1 serving' AS nombre_en, 85.0 AS gramos, 'USDA FDC 174223: 3 oz' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '10002' AS c, '1 ración' AS nombre, '1 serving' AS nombre_en, 85.0 AS gramos, 'USDA FDC 174223: 3 oz' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '10018' AS c, '1 ración' AS nombre, '1 serving' AS nombre_en, 85.0 AS gramos, 'USDA FDC 174218: 3 oz' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '10079' AS c, '1 ración' AS nombre, '1 serving' AS nombre_en, 85.0 AS gramos, 'USDA FDC 174218: 3 oz' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '10016' AS c, '1 ración' AS nombre, '1 serving' AS nombre_en, 85.0 AS gramos, 'USDA FDC 174223: 3 oz' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '10017' AS c, '1 almeja' AS nombre, '1 clam' AS nombre_en, 14.5 AS gramos, 'USDA FDC 174214: 1 medium' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '10017' AS c, '1 ración' AS nombre, '1 serving' AS nombre_en, 85.0 AS gramos, 'USDA FDC 174214: 3 oz' AS fuente_peso, 2 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '26046' AS c, '1 barrita' AS nombre, '1 stick' AS nombre_en, 17.0 AS gramos, 'USDA FDC 173702: 3 oz / 5 barritas' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '26046' AS c, '1 ración' AS nombre, '1 serving' AS nombre_en, 85.0 AS gramos, 'USDA FDC 173702: 3 oz' AS fuente_peso, 2 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '26028' AS c, '1 varita' AS nombre, '1 fish finger' AS nombre_en, 28.0 AS gramos, 'USDA FDC 174195: 1 stick' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '22000' AS c, '1 huevo L' AS nombre, '1 large egg' AS nombre_en, 50.0 AS gramos, 'USDA FDC 171287: 1 large' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '22000' AS c, '1 huevo M' AS nombre, '1 medium egg' AS nombre_en, 44.0 AS gramos, 'USDA FDC 171287: 1 medium' AS fuente_peso, 2 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '22000' AS c, '1 huevo XL' AS nombre, '1 extra large egg' AS nombre_en, 56.0 AS gramos, 'USDA FDC 171287: 1 extra large' AS fuente_peso, 3 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '22010' AS c, '1 huevo L' AS nombre, '1 large egg' AS nombre_en, 50.0 AS gramos, 'USDA FDC 173424: 1 large' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '22011' AS c, '1 huevo L' AS nombre, '1 large egg' AS nombre_en, 50.0 AS gramos, 'USDA FDC 171287: 1 large' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '22505' AS c, '1 huevo L' AS nombre, '1 large egg' AS nombre_en, 46.0 AS gramos, 'USDA FDC 173423: 1 large' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'USDA' AS f, '173423' AS c, '1 huevo L' AS nombre, '1 large egg' AS nombre_en, 46.0 AS gramos, 'USDA FDC 173423: 1 large' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '22502' AS c, '1 huevo' AS nombre, '1 egg' AS nombre_en, 61.0 AS gramos, 'USDA FDC 172187: 1 large' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '22001' AS c, '1 clara' AS nombre, '1 egg white' AS nombre_en, 33.0 AS gramos, 'USDA FDC 172183: 1 large' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '22002' AS c, '1 yema' AS nombre, '1 egg yolk' AS nombre_en, 17.0 AS gramos, 'USDA FDC 172184: 1 large' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '22500' AS c, '1 tortilla de 2 huevos' AS nombre, '1 two-egg omelette' AS nombre_en, 122.0 AS gramos, 'USDA FDC 172187: 2 large' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '22510' AS c, '1 ración' AS nombre, '1 serving' AS nombre_en, 150.0 AS gramos, 'Pincho o ración de bar habitual en España (estimación propia)' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '22050' AS c, '1 huevo' AS nombre, '1 egg' AS nombre_en, 9.0 AS gramos, 'USDA FDC 172191: 1 egg' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '19041' AS c, '1 vaso (250 ml)' AS nombre, '1 glass (250 ml)' AS nombre_en, 254.0 AS gramos, 'USDA FDC 171267: 1 cup (240 ml) = 244 g, a 250 ml' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '19041' AS c, '1 taza (240 ml)' AS nombre, '1 cup (240 ml)' AS nombre_en, 244.0 AS gramos, 'USDA FDC 171267: 1 cup' AS fuente_peso, 2 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '19023' AS c, '1 vaso (250 ml)' AS nombre, '1 glass (250 ml)' AS nombre_en, 254.0 AS gramos, 'USDA FDC 171265: 1 cup (240 ml) = 244 g, a 250 ml' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '19050' AS c, '1 vaso (250 ml)' AS nombre, '1 glass (250 ml)' AS nombre_en, 254.0 AS gramos, 'USDA FDC 171267: 1 cup (240 ml) = 244 g, a 250 ml' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '19060' AS c, '1 vaso (250 ml)' AS nombre, '1 glass (250 ml)' AS nombre_en, 254.0 AS gramos, 'USDA FDC 171267: 1 cup (240 ml) = 244 g, a 250 ml' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '19593' AS c, '1 yogur' AS nombre, '1 pot' AS nombre_en, 125.0 AS gramos, 'Envase de 125 g, el estándar del yogur en España' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '19594' AS c, '1 yogur' AS nombre, '1 pot' AS nombre_en, 125.0 AS gramos, 'Envase de 125 g, el estándar del yogur en España' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '19599' AS c, '1 yogur' AS nombre, '1 pot' AS nombre_en, 125.0 AS gramos, 'Envase de 125 g, el estándar del yogur en España' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '19575' AS c, '1 yogur' AS nombre, '1 pot' AS nombre_en, 125.0 AS gramos, 'Envase de 125 g, el estándar del yogur en España' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '19587' AS c, '1 yogur' AS nombre, '1 pot' AS nombre_en, 125.0 AS gramos, 'Envase de 125 g, el estándar del yogur en España' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '19860' AS c, '1 yogur' AS nombre, '1 pot' AS nombre_en, 125.0 AS gramos, 'Envase de 125 g, el estándar del yogur en España' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'USDA' AS f, '170894' AS c, '1 tarrina' AS nombre, '1 container' AS nombre_en, 170.0 AS gramos, 'USDA FDC 170894: 1 container' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '19724' AS c, '1 tarrina' AS nombre, '1 container' AS nombre_en, 150.0 AS gramos, 'USDA FDC 170902: 1 container (5.3 oz)' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '19865' AS c, '1 vaso (250 ml)' AS nombre, '1 glass (250 ml)' AS nombre_en, 253.0 AS gramos, 'USDA FDC 170904: 1 cup (240 ml) = 243 g, a 250 ml' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '19644' AS c, '1 cucharada' AS nombre, '1 tablespoon' AS nombre_en, 15.0 AS gramos, 'USDA FDC 173418: 1 tbsp' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '19644' AS c, '1 tarrina' AS nombre, '1 container' AS nombre_en, 125.0 AS gramos, 'Envase de 125 g, el estándar del yogur en España' AS fuente_peso, 2 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '19646' AS c, '1 tarrina' AS nombre, '1 container' AS nombre_en, 125.0 AS gramos, 'Envase de 125 g, el estándar del yogur en España' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'USDA' AS f, '173417' AS c, '1 taza' AS nombre, '1 cup' AS nombre_en, 226.0 AS gramos, 'USDA FDC 173417: 1 cup (not packed)' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'USDA' AS f, '173417' AS c, '1 ración' AS nombre, '1 serving' AS nombre_en, 113.0 AS gramos, 'USDA FDC 173417: 4 oz' AS fuente_peso, 2 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '12068' AS c, '1 cucharada' AS nombre, '1 tablespoon' AS nombre_en, 14.5 AS gramos, 'USDA FDC 173418: 1 tbsp' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '12069' AS c, '1 cucharada' AS nombre, '1 tablespoon' AS nombre_en, 15.0 AS gramos, 'USDA FDC 169079: 1 tbsp' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'USDA' AS f, '171248' AS c, '1 cucharada' AS nombre, '1 tablespoon' AS nombre_en, 15.0 AS gramos, 'USDA FDC 171248: 0.5 cup = 124 g / 8' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'USDA' AS f, '171248' AS c, 'Media taza' AS nombre, 'Half cup' AS nombre_en, 124.0 AS gramos, 'USDA FDC 171248: 0.5 cup' AS fuente_peso, 2 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '19590' AS c, '1 bola' AS nombre, '1 ball' AS nombre_en, 125.0 AS gramos, 'Bola de 125 g, el envase estándar en España' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '19590' AS c, '1 ración' AS nombre, '1 serving' AS nombre_en, 28.0 AS gramos, 'USDA FDC 170845: 1 oz' AS fuente_peso, 2 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '12072' AS c, '1 burrata' AS nombre, '1 burrata' AS nombre_en, 125.0 AS gramos, 'Envase de 125 g, el habitual en España' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '12120' AS c, '1 cucharada rallado' AS nombre, '1 tablespoon grated' AS nombre_en, 5.0 AS gramos, 'USDA FDC 170848: 1 tbsp grated' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '12120' AS c, '1 ración' AS nombre, '1 serving' AS nombre_en, 28.0 AS gramos, 'USDA FDC 170848: 1 oz' AS fuente_peso, 2 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '12122' AS c, '1 loncha' AS nombre, '1 slice' AS nombre_en, 20.0 AS gramos, 'USDA FDC 171251: 1 slice (3/4 oz)' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '12122' AS c, '1 ración' AS nombre, '1 serving' AS nombre_en, 28.0 AS gramos, 'USDA FDC 171251: 1 oz' AS fuente_peso, 2 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '12747' AS c, '1 loncha' AS nombre, '1 slice' AS nombre_en, 20.0 AS gramos, 'USDA FDC 171251: 1 slice (3/4 oz)' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '12115' AS c, '1 loncha' AS nombre, '1 slice' AS nombre_en, 28.0 AS gramos, 'USDA FDC 171251: 1 slice (1 oz)' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '12736' AS c, '1 loncha' AS nombre, '1 slice' AS nombre_en, 28.0 AS gramos, 'USDA FDC 171251: 1 slice (1 oz)' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '12729' AS c, '1 loncha' AS nombre, '1 slice' AS nombre_en, 28.0 AS gramos, 'USDA FDC 171251: 1 slice (1 oz)' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '12726' AS c, '1 loncha' AS nombre, '1 slice' AS nombre_en, 21.0 AS gramos, 'USDA FDC 170899: 1 slice (3/4 oz)' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '12020' AS c, '1 ración' AS nombre, '1 serving' AS nombre_en, 28.0 AS gramos, 'USDA FDC 172177: 1 oz' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '12001' AS c, '1 porción' AS nombre, '1 wedge' AS nombre_en, 38.0 AS gramos, 'USDA FDC 172178: 1 wedge' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '12812' AS c, '1 rodaja' AS nombre, '1 slice' AS nombre_en, 28.0 AS gramos, 'USDA FDC 173435: 1 oz' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '12066' AS c, '1 ración' AS nombre, '1 serving' AS nombre_en, 28.0 AS gramos, 'USDA FDC 173420: 1 oz' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '12300' AS c, '1 loncha' AS nombre, '1 slice' AS nombre_en, 21.0 AS gramos, 'USDA FDC 171290: 1 slice (3/4 oz)' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '12303' AS c, '1 quesito' AS nombre, '1 triangle' AS nombre_en, 16.0 AS gramos, 'Porción de 16 g, la estándar de los quesitos en España' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '12775' AS c, '1 puñado' AS nombre, '1 handful' AS nombre_en, 28.0 AS gramos, 'USDA FDC 171251: 1 oz' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '16400' AS c, '1 cucharada' AS nombre, '1 tablespoon' AS nombre_en, 14.2 AS gramos, 'USDA FDC 173410: 1 tbsp' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '16400' AS c, '1 nuez' AS nombre, '1 pat' AS nombre_en, 5.0 AS gramos, 'USDA FDC 173410: 1 pat' AS fuente_peso, 2 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '16415' AS c, '1 cucharada' AS nombre, '1 tablespoon' AS nombre_en, 14.2 AS gramos, 'USDA FDC 173410: 1 tbsp' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '19417' AS c, '1 cucharada' AS nombre, '1 tablespoon' AS nombre_en, 15.0 AS gramos, 'USDA FDC 170859: 1 tbsp' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '19417' AS c, '1 brik (200 ml)' AS nombre, '1 carton (200 ml)' AS nombre_en, 200.0 AS gramos, 'Brik de 200 ml, el estándar en España (densidad ≈ 1 g/ml)' AS fuente_peso, 2 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '19436' AS c, '1 cucharada' AS nombre, '1 tablespoon' AS nombre_en, 15.0 AS gramos, 'USDA FDC 170859: 1 tbsp' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'USDA' AS f, '170859' AS c, '1 cucharada' AS nombre, '1 tablespoon' AS nombre_en, 15.0 AS gramos, 'USDA FDC 170859: 1 tbsp' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '19674' AS c, '1 flan' AS nombre, '1 flan' AS nombre_en, 100.0 AS gramos, 'Envase de 100 g, el estándar del flan en España' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '39229' AS c, '1 natillas' AS nombre, '1 pot' AS nombre_en, 125.0 AS gramos, 'Envase de 125 g, el estándar en España' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '39212' AS c, '1 arroz con leche' AS nombre, '1 pot' AS nombre_en, 125.0 AS gramos, 'Envase de 125 g, el estándar en España' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '39200' AS c, '1 natillas' AS nombre, '1 pot' AS nombre_en, 125.0 AS gramos, 'Envase de 125 g, el estándar en España' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '20585' AS c, '1 ración en seco' AS nombre, '1 dry serving' AS nombre_en, 60.0 AS gramos, 'USDA FDC 172421: 1 cup cocido = 198 g ≈ 80 g en seco' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '20587' AS c, '1 plato' AS nombre, '1 plate' AS nombre_en, 198.0 AS gramos, 'USDA FDC 172421: 1 cup' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '20587' AS c, '1 cucharada' AS nombre, '1 tablespoon' AS nombre_en, 12.3 AS gramos, 'USDA FDC 172421: 1 tbsp' AS fuente_peso, 2 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '20535' AS c, '1 ración en seco' AS nombre, '1 dry serving' AS nombre_en, 60.0 AS gramos, 'USDA FDC 172421: 1 cup cocido = 198 g ≈ 80 g en seco' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '20510' AS c, '1 bote escurrido' AS nombre, '1 drained jar' AS nombre_en, 250.0 AS gramos, 'Bote de 400 g, el estándar en España: 250 g escurridos declarados' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '20510' AS c, '1 plato' AS nombre, '1 plate' AS nombre_en, 198.0 AS gramos, 'USDA FDC 172421: 1 cup' AS fuente_peso, 2 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '20516' AS c, '1 ración en seco' AS nombre, '1 dry serving' AS nombre_en, 60.0 AS gramos, 'USDA FDC 173757: 1 cup cocido = 164 g' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '20507' AS c, '1 plato' AS nombre, '1 plate' AS nombre_en, 164.0 AS gramos, 'USDA FDC 173757: 1 cup' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '20532' AS c, '1 bote escurrido' AS nombre, '1 drained jar' AS nombre_en, 250.0 AS gramos, 'Bote de 400 g, el estándar en España: 250 g escurridos declarados' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '20532' AS c, '1 plato' AS nombre, '1 plate' AS nombre_en, 164.0 AS gramos, 'USDA FDC 173757: 1 cup' AS fuente_peso, 2 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '20501' AS c, '1 ración en seco' AS nombre, '1 dry serving' AS nombre_en, 60.0 AS gramos, 'USDA FDC 175203: 1 cup cocido = 179 g' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '20502' AS c, '1 plato' AS nombre, '1 plate' AS nombre_en, 179.0 AS gramos, 'USDA FDC 175203: 1 cup' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '20511' AS c, '1 bote escurrido' AS nombre, '1 drained jar' AS nombre_en, 250.0 AS gramos, 'Bote de 400 g, el estándar en España: 250 g escurridos declarados' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '20525' AS c, '1 ración en seco' AS nombre, '1 dry serving' AS nombre_en, 60.0 AS gramos, 'USDA FDC 173740: 1 cup cocido = 177 g' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '20503' AS c, '1 plato' AS nombre, '1 plate' AS nombre_en, 177.0 AS gramos, 'USDA FDC 173740: 1 cup' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '20524' AS c, '1 bote escurrido' AS nombre, '1 drained jar' AS nombre_en, 250.0 AS gramos, 'Bote de 400 g, el estándar en España: 250 g escurridos declarados' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '20500' AS c, '1 plato' AS nombre, '1 plate' AS nombre_en, 170.0 AS gramos, 'USDA FDC 173753: 1 cup' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '20072' AS c, 'Media taza' AS nombre, 'Half cup' AS nombre_en, 80.0 AS gramos, 'USDA FDC 170017: 0.5 cup' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '20124' AS c, 'Media taza' AS nombre, 'Half cup' AS nombre_en, 80.0 AS gramos, 'USDA FDC 170017: 0.5 cup' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '20901' AS c, '1 taza' AS nombre, '1 cup' AS nombre_en, 186.0 AS gramos, 'USDA FDC 174270: 1 cup' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'USDA' AS f, '168411' AS c, '1 taza' AS nombre, '1 cup' AS nombre_en, 155.0 AS gramos, 'USDA FDC 168411: 1 cup' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '20904' AS c, '1 ración' AS nombre, '1 serving' AS nombre_en, 81.0 AS gramos, 'USDA FDC 172475: 0.25 block' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'USDA' AS f, '174272' AS c, '1 taza' AS nombre, '1 cup' AS nombre_en, 166.0 AS gramos, 'USDA FDC 174272: 1 cup' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '20591' AS c, '1 ración' AS nombre, '1 serving' AS nombre_en, 85.0 AS gramos, 'USDA FDC 174030: 3 oz' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '25621' AS c, '1 cucharada' AS nombre, '1 tablespoon' AS nombre_en, 15.0 AS gramos, 'USDA FDC 174289: 1 tbsp' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '9100' AS c, '1 ración en seco' AS nombre, '1 dry serving' AS nombre_en, 80.0 AS gramos, 'USDA FDC 168877: 1 cup = 185 g' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '9104' AS c, '1 taza' AS nombre, '1 cup' AS nombre_en, 158.0 AS gramos, 'USDA FDC 168878: 1 cup' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '9102' AS c, '1 ración en seco' AS nombre, '1 dry serving' AS nombre_en, 80.0 AS gramos, 'USDA FDC 168877: 1 cup = 185 g' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '9103' AS c, '1 taza' AS nombre, '1 cup' AS nombre_en, 202.0 AS gramos, 'USDA FDC 169704: 1 cup' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '9125' AS c, '1 taza' AS nombre, '1 cup' AS nombre_en, 158.0 AS gramos, 'USDA FDC 168878: 1 cup' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '9810' AS c, '1 ración en seco' AS nombre, '1 dry serving' AS nombre_en, 80.0 AS gramos, 'USDA FDC 169736: 2 oz = 57 g' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '9811' AS c, '1 taza de espaguetis' AS nombre, '1 cup spaghetti' AS nombre_en, 124.0 AS gramos, 'USDA FDC 169737: 1 cup spaghetti not packed' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '9811' AS c, '1 taza de macarrones' AS nombre, '1 cup penne' AS nombre_en, 107.0 AS gramos, 'USDA FDC 169737: 1 cup penne' AS fuente_peso, 2 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '9870' AS c, '1 ración en seco' AS nombre, '1 dry serving' AS nombre_en, 80.0 AS gramos, 'USDA FDC 169736: 2 oz = 57 g' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '9871' AS c, '1 taza' AS nombre, '1 cup' AS nombre_en, 124.0 AS gramos, 'USDA FDC 169737: 1 cup spaghetti not packed' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '9815' AS c, '1 ración' AS nombre, '1 serving' AS nombre_en, 125.0 AS gramos, 'Envase de 250 g para dos raciones, el habitual en España' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '25216' AS c, '1 taza' AS nombre, '1 cup' AS nombre_en, 124.0 AS gramos, 'USDA FDC 169737: 1 cup spaghetti not packed' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '9863' AS c, '1 ración en seco' AS nombre, '1 dry serving' AS nombre_en, 43.0 AS gramos, 'USDA FDC 168905: 1.5 oz' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '9901' AS c, '1 taza' AS nombre, '1 cup' AS nombre_en, 176.0 AS gramos, 'USDA FDC 168914: 1 cup' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '9610' AS c, '1 ración en seco' AS nombre, '1 dry serving' AS nombre_en, 60.0 AS gramos, 'USDA FDC 169700: 1 oz dry yields' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '9683' AS c, '1 taza' AS nombre, '1 cup' AS nombre_en, 157.0 AS gramos, 'USDA FDC 169700: 1 cup, cooked' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '9340' AS c, '1 ración en seco' AS nombre, '1 dry serving' AS nombre_en, 60.0 AS gramos, 'USDA FDC 168917: 1 cup cocido = 185 g' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '9341' AS c, '1 taza' AS nombre, '1 cup' AS nombre_en, 185.0 AS gramos, 'USDA FDC 168917: 1 cup' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '9691' AS c, '1 taza' AS nombre, '1 cup' AS nombre_en, 182.0 AS gramos, 'USDA FDC 170287: 1 cup' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '9615' AS c, '1 taza' AS nombre, '1 cup' AS nombre_en, 240.0 AS gramos, 'USDA FDC 169761: 1 cup de agua equivalente (estimación propia)' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '32140' AS c, '1 taza' AS nombre, '1 cup' AS nombre_en, 81.0 AS gramos, 'USDA FDC 173904: 1 cup' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '32140' AS c, '1 cucharada' AS nombre, '1 tablespoon' AS nombre_en, 5.0 AS gramos, 'USDA FDC 173904: 1 cup = 81 g / 16' AS fuente_peso, 2 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '32140' AS c, '1 ración' AS nombre, '1 serving' AS nombre_en, 40.0 AS gramos, 'USDA FDC 173904: 0.33 cup = 27 g' AS fuente_peso, 3 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '32004' AS c, '1 taza' AS nombre, '1 cup' AS nombre_en, 122.0 AS gramos, 'USDA FDC 171646: 1 cup' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '32004' AS c, '1 ración' AS nombre, '1 serving' AS nombre_en, 40.0 AS gramos, 'USDA FDC 171646: 1 oz = 28 g' AS fuente_peso, 2 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '32005' AS c, '1 taza' AS nombre, '1 cup' AS nombre_en, 28.0 AS gramos, 'USDA FDC 174648: 1 cup (1 NLEA serving)' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '32003' AS c, '1 ración' AS nombre, '1 serving' AS nombre_en, 30.0 AS gramos, 'USDA FDC 171646: 1 oz ≈ 28 g' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '7001' AS c, '1 rebanada' AS nombre, '1 slice' AS nombre_en, 28.0 AS gramos, 'USDA FDC 172818: 1 oz' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '7001' AS c, '1 barra' AS nombre, '1 baguette' AS nombre_en, 250.0 AS gramos, 'Barra de 250 g, la estándar en España' AS fuente_peso, 2 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '7001' AS c, 'Media barra' AS nombre, 'Half baguette' AS nombre_en, 125.0 AS gramos, 'Barra de 250 g, la estándar en España' AS fuente_peso, 3 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '7110' AS c, '1 rebanada' AS nombre, '1 slice' AS nombre_en, 32.0 AS gramos, 'USDA FDC 172688: 1 slice' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '7100' AS c, '1 rebanada' AS nombre, '1 slice' AS nombre_en, 32.0 AS gramos, 'USDA FDC 172688: 1 slice' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '7255' AS c, '1 rebanada' AS nombre, '1 slice' AS nombre_en, 26.0 AS gramos, 'USDA FDC 168013: 1 slice regular' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '7125' AS c, '1 rebanada' AS nombre, '1 slice' AS nombre_en, 32.0 AS gramos, 'USDA FDC 172684: 1 slice, regular' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '7200' AS c, '1 rebanada' AS nombre, '1 slice' AS nombre_en, 25.0 AS gramos, 'USDA FDC 172818: 1 slice' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '7111' AS c, '1 rebanada' AS nombre, '1 slice' AS nombre_en, 32.0 AS gramos, 'USDA FDC 172688: 1 slice' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '7130' AS c, '1 rebanada' AS nombre, '1 slice' AS nombre_en, 25.0 AS gramos, 'USDA FDC 172818: 1 slice' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '7259' AS c, '1 pan' AS nombre, '1 bun' AS nombre_en, 44.0 AS gramos, 'USDA FDC 172796: 1 roll' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '7180' AS c, '1 pan grande' AS nombre, '1 large pita' AS nombre_en, 60.0 AS gramos, 'USDA FDC 174915: 1 pita, large' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '7180' AS c, '1 pan pequeño' AS nombre, '1 small pita' AS nombre_en, 28.0 AS gramos, 'USDA FDC 174915: 1 pita, small' AS fuente_peso, 2 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '7815' AS c, '1 tortilla mediana' AS nombre, '1 medium tortilla' AS nombre_en, 49.0 AS gramos, 'USDA FDC 175037: 1 tortilla (approx 7-8" dia)' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '7815' AS c, '1 tortilla grande' AS nombre, '1 large tortilla' AS nombre_en, 72.0 AS gramos, 'USDA FDC 175037: 1 tortilla (approx 10" dia)' AS fuente_peso, 2 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '7813' AS c, '1 tortilla' AS nombre, '1 tortilla' AS nombre_en, 24.0 AS gramos, 'USDA FDC 175036: 1 tortilla' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '7300' AS c, '1 tostada' AS nombre, '1 toast' AS nombre_en, 8.0 AS gramos, 'Tostada de 8 g, la habitual en los paquetes españoles' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '7340' AS c, '1 tostada' AS nombre, '1 toast' AS nombre_en, 8.0 AS gramos, 'Tostada de 8 g, la habitual en los paquetes españoles' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '7352' AS c, '1 tortita' AS nombre, '1 cake' AS nombre_en, 9.0 AS gramos, 'USDA FDC 170250: 1 cake' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '7354' AS c, '1 tortita' AS nombre, '1 cake' AS nombre_en, 9.0 AS gramos, 'USDA FDC 170250: 1 cake' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '7525' AS c, '1 ración' AS nombre, '1 serving' AS nombre_en, 28.0 AS gramos, 'USDA FDC 174982: 1 oz' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '7351' AS c, '1 cracker' AS nombre, '1 cracker' AS nombre_en, 4.0 AS gramos, 'USDA FDC 174982: 1 cracker, rectangular' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '9435' AS c, '1 taza' AS nombre, '1 cup' AS nombre_en, 125.0 AS gramos, 'USDA FDC 168894: 1 cup' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '9435' AS c, '1 cucharada' AS nombre, '1 tablespoon' AS nombre_en, 8.0 AS gramos, 'USDA FDC 168894: 1 cup = 125 g / 16' AS fuente_peso, 2 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '9415' AS c, '1 taza' AS nombre, '1 cup' AS nombre_en, 125.0 AS gramos, 'USDA FDC 168894: 1 cup' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '7615' AS c, '1 cruasán mediano' AS nombre, '1 medium croissant' AS nombre_en, 57.0 AS gramos, 'USDA FDC 174987: 1 croissant, medium' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '7615' AS c, '1 cruasán pequeño' AS nombre, '1 small croissant' AS nombre_en, 42.0 AS gramos, 'USDA FDC 174987: 1 croissant, small' AS fuente_peso, 2 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '7730' AS c, '1 napolitana' AS nombre, '1 pastry' AS nombre_en, 57.0 AS gramos, 'USDA FDC 174987: 1 croissant, medium' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '7746' AS c, '1 ración' AS nombre, '1 serving' AS nombre_en, 57.0 AS gramos, 'USDA FDC 174987: 1 croissant, medium' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '4008' AS c, '1 patata mediana' AS nombre, '1 medium potato' AS nombre_en, 136.0 AS gramos, 'USDA FDC 170438: 1 potato (2-1/2" dia)' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '4003' AS c, '1 patata mediana' AS nombre, '1 medium potato' AS nombre_en, 136.0 AS gramos, 'USDA FDC 170438: 1 potato (2-1/2" dia)' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '4003' AS c, 'Media taza' AS nombre, 'Half cup' AS nombre_en, 78.0 AS gramos, 'USDA FDC 170438: 0.5 cup' AS fuente_peso, 2 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '4026' AS c, '1 patata mediana' AS nombre, '1 medium potato' AS nombre_en, 136.0 AS gramos, 'USDA FDC 170438: 1 potato (2-1/2" dia)' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '4018' AS c, '1 taza' AS nombre, '1 cup' AS nombre_en, 210.0 AS gramos, 'USDA FDC 170493: 1 cup' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '4032' AS c, '1 ración' AS nombre, '1 serving' AS nombre_en, 85.0 AS gramos, 'USDA FDC 168948: 3 oz' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '4101' AS c, '1 boniato mediano' AS nombre, '1 medium sweet potato' AS nombre_en, 130.0 AS gramos, 'USDA FDC 168482: 1 sweetpotato, 5" long' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '4102' AS c, '1 boniato mediano' AS nombre, '1 medium sweet potato' AS nombre_en, 130.0 AS gramos, 'USDA FDC 168482: 1 sweetpotato, 5" long' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '25510' AS c, '1 ración' AS nombre, '1 serving' AS nombre_en, 200.0 AS gramos, 'Envase de 400 g para dos raciones, el habitual en España' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '13005' AS c, '1 plátano mediano' AS nombre, '1 medium banana' AS nombre_en, 118.0 AS gramos, 'USDA FDC 173944: 1 medium' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '13005' AS c, '1 plátano pequeño' AS nombre, '1 small banana' AS nombre_en, 101.0 AS gramos, 'USDA FDC 173944: 1 small' AS fuente_peso, 2 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '13005' AS c, '1 plátano grande' AS nombre, '1 large banana' AS nombre_en, 136.0 AS gramos, 'USDA FDC 173944: 1 large' AS fuente_peso, 3 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '13039' AS c, '1 manzana mediana' AS nombre, '1 medium apple' AS nombre_en, 182.0 AS gramos, 'USDA FDC 171688: 1 medium' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '13039' AS c, '1 manzana pequeña' AS nombre, '1 small apple' AS nombre_en, 149.0 AS gramos, 'USDA FDC 171688: 1 small' AS fuente_peso, 2 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '13037' AS c, '1 pera mediana' AS nombre, '1 medium pear' AS nombre_en, 178.0 AS gramos, 'USDA FDC 169118: 1 medium' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '13034' AS c, '1 naranja mediana' AS nombre, '1 medium orange' AS nombre_en, 131.0 AS gramos, 'USDA FDC 169097: 1 fruit' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '13034' AS c, '1 naranja grande' AS nombre, '1 large orange' AS nombre_en, 184.0 AS gramos, 'USDA FDC 169097: 1 large' AS fuente_peso, 2 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '13024' AS c, '1 mandarina' AS nombre, '1 mandarin' AS nombre_en, 88.0 AS gramos, 'USDA FDC 169105: 1 medium' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '13043' AS c, '1 melocotón mediano' AS nombre, '1 medium peach' AS nombre_en, 150.0 AS gramos, 'USDA FDC 169928: 1 medium' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '13399' AS c, '1 nectarina mediana' AS nombre, '1 medium nectarine' AS nombre_en, 142.0 AS gramos, 'USDA FDC 169914: 1 medium' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '13000' AS c, '1 albaricoque' AS nombre, '1 apricot' AS nombre_en, 35.0 AS gramos, 'USDA FDC 171697: 1 apricot' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '13100' AS c, '1 ciruela' AS nombre, '1 plum' AS nombre_en, 66.0 AS gramos, 'USDA FDC 169949: 1 fruit' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '13008' AS c, '1 cereza' AS nombre, '1 cherry' AS nombre_en, 8.2 AS gramos, 'USDA FDC 171719: 1 cherry' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '13008' AS c, '1 taza' AS nombre, '1 cup' AS nombre_en, 154.0 AS gramos, 'USDA FDC 171719: 1 cup, without pits' AS fuente_peso, 2 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '13014' AS c, '1 fresa' AS nombre, '1 strawberry' AS nombre_en, 12.0 AS gramos, 'USDA FDC 167762: 1 medium' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '13014' AS c, '1 taza' AS nombre, '1 cup' AS nombre_en, 144.0 AS gramos, 'USDA FDC 167762: 1 cup, whole' AS fuente_peso, 2 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '13015' AS c, '1 taza' AS nombre, '1 cup' AS nombre_en, 123.0 AS gramos, 'USDA FDC 167755: 1 cup' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '13028' AS c, '1 taza' AS nombre, '1 cup' AS nombre_en, 148.0 AS gramos, 'USDA FDC 171711: 1 cup' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '13029' AS c, '1 taza' AS nombre, '1 cup' AS nombre_en, 144.0 AS gramos, 'USDA FDC 173946: 1 cup' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '13395' AS c, '10 uvas' AS nombre, '10 grapes' AS nombre_en, 49.0 AS gramos, 'USDA FDC 174683: 10 grapes' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '13395' AS c, '1 taza' AS nombre, '1 cup' AS nombre_en, 151.0 AS gramos, 'USDA FDC 174683: 1 cup' AS fuente_peso, 2 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '13026' AS c, '1 tajada' AS nombre, '1 wedge' AS nombre_en, 102.0 AS gramos, 'USDA FDC 169092: 1 wedge, large' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '13742' AS c, '1 tajada' AS nombre, '1 wedge' AS nombre_en, 160.0 AS gramos, 'USDA FDC 169911: 1 wedge (1/8 of 6" to 7" dia melon)' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '13036' AS c, '1 tajada' AS nombre, '1 wedge' AS nombre_en, 286.0 AS gramos, 'USDA FDC 167765: 1 wedge' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '13002' AS c, '1 rodaja' AS nombre, '1 slice' AS nombre_en, 84.0 AS gramos, 'USDA FDC 169124: 1 slice (3-1/2" dia x 3/4" thick)' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '13025' AS c, '1 mango' AS nombre, '1 mango' AS nombre_en, 336.0 AS gramos, 'USDA FDC 169910: 1 fruit without refuse' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '13021' AS c, '1 kiwi' AS nombre, '1 kiwi' AS nombre_en, 69.0 AS gramos, 'USDA FDC 168153: 1 fruit' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '13035' AS c, '1 taza' AS nombre, '1 cup' AS nombre_en, 145.0 AS gramos, 'USDA FDC 169926: 1 cup 1" pieces' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '13012' AS c, '1 higo' AS nombre, '1 fig' AS nombre_en, 50.0 AS gramos, 'USDA FDC 173021: 1 medium' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '13018' AS c, '1 granada' AS nombre, '1 pomegranate' AS nombre_en, 282.0 AS gramos, 'USDA FDC 169134: 1 pomegranate' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '13018' AS c, 'Media taza de granos' AS nombre, 'Half cup of seeds' AS nombre_en, 87.0 AS gramos, 'USDA FDC 169134: 0.5 cup arils' AS fuente_peso, 2 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '13009' AS c, '1 limón' AS nombre, '1 lemon' AS nombre_en, 58.0 AS gramos, 'USDA FDC 167746: 1 fruit (2-1/8" dia)' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '13040' AS c, 'Medio pomelo' AS nombre, 'Half grapefruit' AS nombre_en, 123.0 AS gramos, 'USDA FDC 174673: 0.5 fruit' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '13066' AS c, '1 caqui' AS nombre, '1 persimmon' AS nombre_en, 168.0 AS gramos, 'USDA FDC 169941: 1 fruit' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '13564' AS c, '1 níspero' AS nombre, '1 loquat' AS nombre_en, 16.0 AS gramos, 'USDA FDC 169908: 1 medium' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '13023' AS c, '1 lichi' AS nombre, '1 lychee' AS nombre_en, 9.6 AS gramos, 'USDA FDC 169086: 1 fruit without refuse' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '13016' AS c, '1 maracuyá' AS nombre, '1 passion fruit' AS nombre_en, 18.0 AS gramos, 'USDA FDC 169108: 1 fruit without refuse' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '13004' AS c, '1 aguacate' AS nombre, '1 avocado' AS nombre_en, 201.0 AS gramos, 'USDA FDC 171705: 1 avocado' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '13004' AS c, 'Medio aguacate' AS nombre, 'Half avocado' AS nombre_en, 100.0 AS gramos, 'USDA FDC 171705: 1 avocado = 201 g / 2' AS fuente_peso, 2 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '15006' AS c, '1 ración' AS nombre, '1 serving' AS nombre_en, 28.0 AS gramos, 'USDA FDC 170567: 1 oz' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '13011' AS c, '1 dátil' AS nombre, '1 date' AS nombre_en, 24.0 AS gramos, 'USDA FDC 168191: 1 date, pitted' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '13046' AS c, '1 puñado' AS nombre, '1 handful' AS nombre_en, 28.0 AS gramos, 'USDA FDC 170567: 1 oz' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '13042' AS c, '1 ciruela pasa' AS nombre, '1 prune' AS nombre_en, 9.5 AS gramos, 'USDA FDC 168162: 1 prune, pitted' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '13001' AS c, '1 orejón' AS nombre, '1 dried apricot half' AS nombre_en, 3.5 AS gramos, 'USDA FDC 173941: 1 half' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '13013' AS c, '1 higo seco' AS nombre, '1 dried fig' AS nombre_en, 8.4 AS gramos, 'USDA FDC 174665: 1 fig' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '13089' AS c, '1 puñado' AS nombre, '1 handful' AS nombre_en, 28.0 AS gramos, 'USDA FDC 170567: 1 oz' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '13038' AS c, '1 tarrina' AS nombre, '1 pot' AS nombre_en, 100.0 AS gramos, 'Envase de 100 g, el estándar de la compota en España' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '13134' AS c, '1 taza' AS nombre, '1 cup' AS nombre_en, 150.0 AS gramos, 'USDA FDC 173944: 1 cup, sliced (estimación por la fruta más abundante)' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '13033' AS c, '1 aceituna' AS nombre, '1 olive' AS nombre_en, 4.4 AS gramos, 'USDA FDC 169094: 1 large' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '13033' AS c, '10 aceitunas' AS nombre, '10 olives' AS nombre_en, 44.0 AS gramos, 'USDA FDC 169094: 1 large × 10' AS fuente_peso, 2 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '13032' AS c, '1 aceituna' AS nombre, '1 olive' AS nombre_en, 4.4 AS gramos, 'USDA FDC 169094: 1 large' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '20385' AS c, '1 tomate mediano' AS nombre, '1 medium tomato' AS nombre_en, 123.0 AS gramos, 'USDA FDC 170457: 1 medium whole' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '20385' AS c, '1 tomate grande' AS nombre, '1 large tomato' AS nombre_en, 182.0 AS gramos, 'USDA FDC 170457: 1 large whole' AS fuente_peso, 2 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '20172' AS c, '1 tomate cherry' AS nombre, '1 cherry tomato' AS nombre_en, 17.0 AS gramos, 'USDA FDC 170457: 1 cherry' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '20048' AS c, '1 lata escurrida' AS nombre, '1 drained can' AS nombre_en, 240.0 AS gramos, 'Lata de 400 g, la estándar en España: 240 g escurridos declarados' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '20170' AS c, '1 cucharada' AS nombre, '1 tablespoon' AS nombre_en, 15.0 AS gramos, 'USDA FDC 171192: 0.5 cup = 132 g / 8 (estimación por volumen)' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '20068' AS c, '1 cucharada' AS nombre, '1 tablespoon' AS nombre_en, 16.0 AS gramos, 'USDA FDC 172241: 1 tbsp de líquido denso (estimación por volumen)' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '20019' AS c, '1 pepino' AS nombre, '1 cucumber' AS nombre_en, 301.0 AS gramos, 'USDA FDC 168409: 1 cucumber' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '20019' AS c, 'Media taza en rodajas' AS nombre, 'Half cup sliced' AS nombre_en, 52.0 AS gramos, 'USDA FDC 168409: 0.5 cup slices' AS fuente_peso, 2 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '20031' AS c, '1 hoja' AS nombre, '1 leaf' AS nombre_en, 24.0 AS gramos, 'USDA FDC 169249: 1 leaf outer' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '20031' AS c, '1 taza' AS nombre, '1 cup' AS nombre_en, 36.0 AS gramos, 'USDA FDC 169249: 1 cup shredded' AS fuente_peso, 2 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '20171' AS c, '1 hoja' AS nombre, '1 leaf' AS nombre_en, 28.0 AS gramos, 'USDA FDC 169247: 1 leaf outer' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '20171' AS c, '1 taza' AS nombre, '1 cup' AS nombre_en, 47.0 AS gramos, 'USDA FDC 169247: 1 cup shredded' AS fuente_peso, 2 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '20200' AS c, '1 taza' AS nombre, '1 cup' AS nombre_en, 36.0 AS gramos, 'USDA FDC 169249: 1 cup shredded' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '20217' AS c, '1 taza' AS nombre, '1 cup' AS nombre_en, 20.0 AS gramos, 'USDA FDC 168462: 1 cup de hoja tierna (estimación por la espinaca, 30 g)' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '20099' AS c, '1 taza' AS nombre, '1 cup' AS nombre_en, 30.0 AS gramos, 'USDA FDC 168462: 1 cup' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '20059' AS c, '1 taza' AS nombre, '1 cup' AS nombre_en, 30.0 AS gramos, 'USDA FDC 168462: 1 cup' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '20027' AS c, 'Media taza' AS nombre, 'Half cup' AS nombre_en, 90.0 AS gramos, 'USDA FDC 168462: 1 bunch 340 g crudo reduce a ≈ 1 cup cocido (estimación propia)' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '20346' AS c, '1 taza' AS nombre, '1 cup' AS nombre_en, 30.0 AS gramos, 'USDA FDC 168462: 1 cup de hoja (estimación por la espinaca)' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '20057' AS c, '1 taza troceado' AS nombre, '1 cup chopped' AS nombre_en, 91.0 AS gramos, 'USDA FDC 170379: 1 cup chopped' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '20351' AS c, '1 taza' AS nombre, '1 cup' AS nombre_en, 91.0 AS gramos, 'USDA FDC 170379: 1 cup chopped' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '20016' AS c, '1 taza troceada' AS nombre, '1 cup chopped' AS nombre_en, 107.0 AS gramos, 'USDA FDC 169986: 1 cup chopped' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '20017' AS c, '1 taza' AS nombre, '1 cup' AS nombre_en, 107.0 AS gramos, 'USDA FDC 169986: 1 cup chopped' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '20058' AS c, '1 taza' AS nombre, '1 cup' AS nombre_en, 88.0 AS gramos, 'USDA FDC 170383: 1 cup' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '20069' AS c, '1 taza picado' AS nombre, '1 cup shredded' AS nombre_en, 70.0 AS gramos, 'USDA FDC 169975: 1 cup, shredded' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '20014' AS c, '1 taza picada' AS nombre, '1 cup shredded' AS nombre_en, 70.0 AS gramos, 'USDA FDC 169975: 1 cup, shredded' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '20009' AS c, '1 zanahoria mediana' AS nombre, '1 medium carrot' AS nombre_en, 61.0 AS gramos, 'USDA FDC 170393: 1 medium' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '20009' AS c, '1 zanahoria grande' AS nombre, '1 large carrot' AS nombre_en, 72.0 AS gramos, 'USDA FDC 170393: 1 large' AS fuente_peso, 2 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '20307' AS c, '1 taza' AS nombre, '1 cup' AS nombre_en, 128.0 AS gramos, 'USDA FDC 170393: 1 cup chopped' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '20020' AS c, '1 calabacín mediano' AS nombre, '1 medium courgette' AS nombre_en, 196.0 AS gramos, 'USDA FDC 169291: 1 medium' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '20021' AS c, '1 taza' AS nombre, '1 cup' AS nombre_en, 113.0 AS gramos, 'USDA FDC 169291: 1 cup, sliced' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '20053' AS c, '1 berenjena' AS nombre, '1 aubergine' AS nombre_en, 458.0 AS gramos, 'USDA FDC 169228: 1 eggplant, peeled' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '20087' AS c, '1 pimiento mediano' AS nombre, '1 medium pepper' AS nombre_en, 119.0 AS gramos, 'USDA FDC 170108: 1 medium' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '20085' AS c, '1 pimiento mediano' AS nombre, '1 medium pepper' AS nombre_en, 119.0 AS gramos, 'USDA FDC 170427: 1 medium' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '20275' AS c, '1 ración' AS nombre, '1 serving' AS nombre_en, 85.0 AS gramos, 'USDA FDC 170108: 3 oz (estimación por peso)' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '20034' AS c, '1 cebolla mediana' AS nombre, '1 medium onion' AS nombre_en, 110.0 AS gramos, 'USDA FDC 170000: 1 medium' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '20034' AS c, '1 cucharada picada' AS nombre, '1 tablespoon chopped' AS nombre_en, 10.0 AS gramos, 'USDA FDC 170000: 1 tbsp chopped' AS fuente_peso, 2 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '20238' AS c, '1 cebolla mediana' AS nombre, '1 medium onion' AS nombre_en, 110.0 AS gramos, 'USDA FDC 170000: 1 medium' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '20035' AS c, 'Media taza' AS nombre, 'Half cup' AS nombre_en, 105.0 AS gramos, 'USDA FDC 170000: 1 cup chopped = 160 g, reducido al cocinar (estimación propia)' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '11000' AS c, '1 diente' AS nombre, '1 clove' AS nombre_en, 3.0 AS gramos, 'USDA FDC 169230: 1 clove' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '20039' AS c, '1 puerro' AS nombre, '1 leek' AS nombre_en, 89.0 AS gramos, 'USDA FDC 169246: 1 leek' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '20023' AS c, '1 rama' AS nombre, '1 stalk' AS nombre_en, 40.0 AS gramos, 'USDA FDC 169988: 1 stalk, medium' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '20056' AS c, '1 champiñón' AS nombre, '1 mushroom' AS nombre_en, 18.0 AS gramos, 'USDA FDC 169251: 1 medium' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '20056' AS c, '1 taza' AS nombre, '1 cup' AS nombre_en, 70.0 AS gramos, 'USDA FDC 169251: 1 cup, pieces or slices' AS fuente_peso, 2 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '20125' AS c, 'Media taza' AS nombre, 'Half cup' AS nombre_en, 78.0 AS gramos, 'USDA FDC 169251: 1 cup whole 96 g, salteado (estimación propia)' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '20114' AS c, '1 taza' AS nombre, '1 cup' AS nombre_en, 70.0 AS gramos, 'USDA FDC 169251: 1 cup, pieces or slices' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '20061' AS c, '1 taza' AS nombre, '1 cup' AS nombre_en, 100.0 AS gramos, 'USDA FDC 169961: 1 cup 1/2" pieces' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '20030' AS c, '1 taza' AS nombre, '1 cup' AS nombre_en, 125.0 AS gramos, 'USDA FDC 169961: 1 cup 1/2" pieces, cocida (estimación propia)' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '20279' AS c, '1 espárrago' AS nombre, '1 spear' AS nombre_en, 16.0 AS gramos, 'USDA FDC 168389: 1 spear, medium' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '20076' AS c, '1 espárrago' AS nombre, '1 spear' AS nombre_en, 20.0 AS gramos, 'USDA FDC 168389: 1 spear, large' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '20052' AS c, '1 alcachofa' AS nombre, '1 artichoke' AS nombre_en, 128.0 AS gramos, 'USDA FDC 169205: 1 artichoke, medium' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '20000' AS c, '1 alcachofa' AS nombre, '1 artichoke' AS nombre_en, 128.0 AS gramos, 'USDA FDC 169205: 1 artichoke, medium' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '20091' AS c, '1 remolacha' AS nombre, '1 beetroot' AS nombre_en, 82.0 AS gramos, 'USDA FDC 169145: 1 beet' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '20003' AS c, '1 remolacha' AS nombre, '1 beetroot' AS nombre_en, 82.0 AS gramos, 'USDA FDC 169145: 1 beet' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '20045' AS c, '1 rábano' AS nombre, '1 radish' AS nombre_en, 4.5 AS gramos, 'USDA FDC 169276: 1 medium' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '20044' AS c, '1 taza' AS nombre, '1 cup' AS nombre_en, 116.0 AS gramos, 'USDA FDC 168448: 1 cup (1" cubes)' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '20138' AS c, '1 taza' AS nombre, '1 cup' AS nombre_en, 140.0 AS gramos, 'USDA FDC 169295: 1 cup, cubes' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '20026' AS c, '1 endibia' AS nombre, '1 head' AS nombre_en, 53.0 AS gramos, 'USDA FDC 170404: 1 head' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '20004' AS c, '1 taza' AS nombre, '1 cup' AS nombre_en, 36.0 AS gramos, 'USDA FDC 169991: 1 cup' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '20028' AS c, '1 bulbo' AS nombre, '1 bulb' AS nombre_en, 234.0 AS gramos, 'USDA FDC 169385: 1 bulb' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '20064' AS c, '1 nabo' AS nombre, '1 turnip' AS nombre_en, 122.0 AS gramos, 'USDA FDC 170465: 1 medium' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '20066' AS c, '1 lata escurrida' AS nombre, '1 drained can' AS nombre_en, 211.0 AS gramos, 'USDA FDC 169214: 1 can (12 oz) yields' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '20066' AS c, 'Media taza' AS nombre, 'Half cup' AS nombre_en, 82.0 AS gramos, 'USDA FDC 169214: 1 cup = 164 g / 2' AS fuente_peso, 2 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '20183' AS c, '1 taza' AS nombre, '1 cup' AS nombre_en, 104.0 AS gramos, 'USDA FDC 169957: 1 cup' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '11014' AS c, '1 cucharada' AS nombre, '1 tablespoon' AS nombre_en, 3.8 AS gramos, 'USDA FDC 170416: 1 tbsp' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '11004' AS c, '1 pepinillo' AS nombre, '1 gherkin' AS nombre_en, 35.0 AS gramos, 'USDA FDC 168558: 1 spear, small' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '25967' AS c, '1 vaso (250 ml)' AS nombre, '1 glass (250 ml)' AS nombre_en, 250.0 AS gramos, 'Densidad ≈ 1 g/ml (estimación propia)' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '15000' AS c, '1 puñado' AS nombre, '1 handful' AS nombre_en, 28.0 AS gramos, 'USDA FDC 170567: 1 oz (23 whole kernels)' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '15000' AS c, '1 almendra' AS nombre, '1 almond' AS nombre_en, 1.2 AS gramos, 'USDA FDC 170567: 1 almond' AS fuente_peso, 2 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '15042' AS c, '1 puñado' AS nombre, '1 handful' AS nombre_en, 28.0 AS gramos, 'USDA FDC 170567: 1 oz' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '15005' AS c, '1 puñado' AS nombre, '1 handful' AS nombre_en, 28.0 AS gramos, 'USDA FDC 170187: 1 oz (14 halves)' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '15005' AS c, '1 nuez' AS nombre, '1 walnut' AS nombre_en, 4.0 AS gramos, 'USDA FDC 170187: 1 cup in shell, edible yield (7 nuts) = 28 g' AS fuente_peso, 2 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '15004' AS c, '1 puñado' AS nombre, '1 handful' AS nombre_en, 28.0 AS gramos, 'USDA FDC 170581: 1 oz (21 whole kernels)' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '15054' AS c, '1 puñado' AS nombre, '1 handful' AS nombre_en, 28.0 AS gramos, 'USDA FDC 170162: 1 oz' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '15044' AS c, '1 puñado' AS nombre, '1 handful' AS nombre_en, 28.0 AS gramos, 'USDA FDC 170184: 1 oz (49 kernels)' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '15001' AS c, '1 puñado' AS nombre, '1 handful' AS nombre_en, 28.0 AS gramos, 'USDA FDC 172430: 1 oz' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '15002' AS c, '1 puñado' AS nombre, '1 handful' AS nombre_en, 28.0 AS gramos, 'USDA FDC 172430: 1 oz' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '15008' AS c, '1 nuez' AS nombre, '1 nut' AS nombre_en, 5.0 AS gramos, 'USDA FDC 170569: 1 kernel' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '15027' AS c, '1 puñado' AS nombre, '1 handful' AS nombre_en, 28.0 AS gramos, 'USDA FDC 170178: 1 oz' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '15025' AS c, '1 cucharada' AS nombre, '1 tablespoon' AS nombre_en, 8.5 AS gramos, 'USDA FDC 170591: 1 cup = 135 g / 16' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '15066' AS c, '1 puñado' AS nombre, '1 handful' AS nombre_en, 28.0 AS gramos, 'USDA FDC 170567: 1 oz' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '15011' AS c, '1 cucharada' AS nombre, '1 tablespoon' AS nombre_en, 9.0 AS gramos, 'USDA FDC 170562: 1 cup = 140 g / 16' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '15064' AS c, '1 cucharada' AS nombre, '1 tablespoon' AS nombre_en, 9.0 AS gramos, 'USDA FDC 170562: 1 cup = 140 g / 16 (estimación por la pipa de girasol)' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '15047' AS c, '1 cucharada' AS nombre, '1 tablespoon' AS nombre_en, 12.0 AS gramos, 'USDA FDC 170554: 1 oz ≈ 2,5 cucharadas' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '15034' AS c, '1 cucharada' AS nombre, '1 tablespoon' AS nombre_en, 10.3 AS gramos, 'USDA FDC 169414: 1 tbsp, whole' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '15010' AS c, '1 cucharada' AS nombre, '1 tablespoon' AS nombre_en, 9.0 AS gramos, 'USDA FDC 170150: 1 tbsp' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '15202' AS c, '1 cucharada' AS nombre, '1 tablespoon' AS nombre_en, 16.0 AS gramos, 'USDA FDC 172470: 2 tbsp = 32 g' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '15007' AS c, '1 cucharada' AS nombre, '1 tablespoon' AS nombre_en, 5.0 AS gramos, 'USDA FDC 170567: 1 oz ≈ 5,5 cucharadas de coco rallado (estimación propia)' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '17270' AS c, '1 cucharada' AS nombre, '1 tablespoon' AS nombre_en, 13.5 AS gramos, 'USDA FDC 171413: 1 tablespoon' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '17270' AS c, '1 cucharadita' AS nombre, '1 teaspoon' AS nombre_en, 4.5 AS gramos, 'USDA FDC 171413: 1 tsp' AS fuente_peso, 2 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '17440' AS c, '1 cucharada' AS nombre, '1 tablespoon' AS nombre_en, 13.5 AS gramos, 'USDA FDC 171413: 1 tablespoon' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '17130' AS c, '1 cucharada' AS nombre, '1 tablespoon' AS nombre_en, 13.5 AS gramos, 'USDA FDC 171413: 1 tablespoon' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '16050' AS c, '1 cucharada' AS nombre, '1 tablespoon' AS nombre_en, 13.6 AS gramos, 'USDA FDC 171412: 1 tbsp' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '16520' AS c, '1 cucharada' AS nombre, '1 tablespoon' AS nombre_en, 12.8 AS gramos, 'USDA FDC 171401: 1 tbsp' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '11054' AS c, '1 cucharada' AS nombre, '1 tablespoon' AS nombre_en, 13.8 AS gramos, 'USDA FDC 171009: 1 tbsp' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '11079' AS c, '1 cucharada' AS nombre, '1 tablespoon' AS nombre_en, 15.0 AS gramos, 'USDA FDC 171009: 1 tbsp (versión light, más densa: estimación propia)' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '11168' AS c, '1 cucharada' AS nombre, '1 tablespoon' AS nombre_en, 13.8 AS gramos, 'USDA FDC 171009: 1 tbsp' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '11108' AS c, '1 cucharada' AS nombre, '1 tablespoon' AS nombre_en, 15.0 AS gramos, 'USDA FDC 172241: 1 tbsp de líquido (estimación por volumen)' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '18045' AS c, '1 vaso (250 ml)' AS nombre, '1 glass (250 ml)' AS nombre_en, 250.0 AS gramos, 'Densidad 1 g/ml' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '18004' AS c, '1 taza' AS nombre, '1 cup' AS nombre_en, 237.0 AS gramos, 'USDA FDC 171890: 1 cup (8 fl oz)' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '18004' AS c, '1 café solo' AS nombre, '1 espresso' AS nombre_en, 30.0 AS gramos, 'USDA FDC 171890: 1 fl oz' AS fuente_peso, 2 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '18151' AS c, '1 taza' AS nombre, '1 cup' AS nombre_en, 237.0 AS gramos, 'USDA FDC 171890: 1 cup (8 fl oz)' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '18154' AS c, '1 taza' AS nombre, '1 cup' AS nombre_en, 237.0 AS gramos, 'USDA FDC 173227: 1 cup (8 fl oz)' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '18155' AS c, '1 taza' AS nombre, '1 cup' AS nombre_en, 237.0 AS gramos, 'USDA FDC 173227: 1 cup (8 fl oz)' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '18022' AS c, '1 taza' AS nombre, '1 cup' AS nombre_en, 237.0 AS gramos, 'USDA FDC 173227: 1 cup (8 fl oz)' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '2013' AS c, '1 vaso (250 ml)' AS nombre, '1 glass (250 ml)' AS nombre_en, 258.0 AS gramos, 'USDA FDC 169098: 1 cup (240 ml) = 248 g, a 250 ml' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '2070' AS c, '1 vaso (250 ml)' AS nombre, '1 glass (250 ml)' AS nombre_en, 258.0 AS gramos, 'USDA FDC 169098: 1 cup (240 ml) = 248 g, a 250 ml' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '2074' AS c, '1 vaso (250 ml)' AS nombre, '1 glass (250 ml)' AS nombre_en, 258.0 AS gramos, 'USDA FDC 169098: 1 cup (240 ml) = 248 g, a 250 ml' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '2016' AS c, '1 vaso (250 ml)' AS nombre, '1 glass (250 ml)' AS nombre_en, 258.0 AS gramos, 'USDA FDC 169098: 1 cup (240 ml) = 248 g, a 250 ml' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '18018' AS c, '1 lata (330 ml)' AS nombre, '1 can (330 ml)' AS nombre_en, 342.0 AS gramos, 'USDA FDC 174852: 1 fl oz = 30,7 g, a 330 ml' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '18010' AS c, '1 lata (330 ml)' AS nombre, '1 can (330 ml)' AS nombre_en, 342.0 AS gramos, 'USDA FDC 174852: 1 fl oz = 30,7 g, a 330 ml' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '18348' AS c, '1 botella (500 ml)' AS nombre, '1 bottle (500 ml)' AS nombre_en, 518.0 AS gramos, 'USDA FDC 174852: 1 fl oz = 30,7 g, a 500 ml' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '18352' AS c, '1 lata (250 ml)' AS nombre, '1 can (250 ml)' AS nombre_en, 262.0 AS gramos, 'USDA FDC 171937: 1 fl oz = 31 g, a 250 ml' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '18352' AS c, '1 lata (500 ml)' AS nombre, '1 can (500 ml)' AS nombre_en, 524.0 AS gramos, 'USDA FDC 171937: 1 fl oz = 31 g, a 500 ml' AS fuente_peso, 2 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '18025' AS c, '1 botella (330 ml)' AS nombre, '1 bottle (330 ml)' AS nombre_en, 330.0 AS gramos, 'Densidad ≈ 1 g/ml' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '18900' AS c, '1 vaso (250 ml)' AS nombre, '1 glass (250 ml)' AS nombre_en, 253.0 AS gramos, 'USDA FDC 173776: 1 cup (240 ml) = 243 g, a 250 ml' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '18899' AS c, '1 vaso (250 ml)' AS nombre, '1 glass (250 ml)' AS nombre_en, 250.0 AS gramos, 'USDA FDC 171942: 8 fl oz = 240 g, a 250 ml' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '18107' AS c, '1 vaso (250 ml)' AS nombre, '1 glass (250 ml)' AS nombre_en, 250.0 AS gramos, 'USDA FDC 174820: 8 fl oz = 240 g, a 250 ml' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '18904' AS c, '1 vaso (250 ml)' AS nombre, '1 glass (250 ml)' AS nombre_en, 250.0 AS gramos, 'USDA FDC 171942: 8 fl oz = 240 g, a 250 ml' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '18104' AS c, '1 taza' AS nombre, '1 cup' AS nombre_en, 254.0 AS gramos, 'USDA FDC 171267: 1 cup (240 ml) = 244 g, a 250 ml' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '5001' AS c, '1 caña (200 ml)' AS nombre, '1 small glass (200 ml)' AS nombre_en, 201.0 AS gramos, 'USDA FDC 168746: 1 fl oz = 29,7 g, a 200 ml' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '5001' AS c, '1 tercio (330 ml)' AS nombre, '1 bottle (330 ml)' AS nombre_en, 331.0 AS gramos, 'USDA FDC 168746: 1 fl oz = 29,7 g, a 330 ml' AS fuente_peso, 2 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '5001' AS c, '1 lata (330 ml)' AS nombre, '1 can (330 ml)' AS nombre_en, 331.0 AS gramos, 'USDA FDC 168746: 1 fl oz = 29,7 g, a 330 ml' AS fuente_peso, 3 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '5214' AS c, '1 copa' AS nombre, '1 glass' AS nombre_en, 147.0 AS gramos, 'USDA FDC 173190: 1 serving (5 fl oz)' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '5215' AS c, '1 copa' AS nombre, '1 glass' AS nombre_en, 147.0 AS gramos, 'USDA FDC 174837: 1 serving (5 fl oz)' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '5216' AS c, '1 copa' AS nombre, '1 glass' AS nombre_en, 147.0 AS gramos, 'USDA FDC 174837: 1 serving (5 fl oz)' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '5201' AS c, '1 copa' AS nombre, '1 glass' AS nombre_en, 147.0 AS gramos, 'USDA FDC 174837: 1 serving (5 fl oz)' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '5003' AS c, '1 vaso' AS nombre, '1 glass' AS nombre_en, 200.0 AS gramos, 'Densidad ≈ 1 g/ml, vaso de 200 ml' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '1017' AS c, '1 vaso' AS nombre, '1 glass' AS nombre_en, 200.0 AS gramos, 'Densidad ≈ 1 g/ml, vaso de 200 ml' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '1007' AS c, '1 vaso' AS nombre, '1 glass' AS nombre_en, 100.0 AS gramos, 'Densidad ≈ 1 g/ml, vaso de 100 ml' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '1005' AS c, '1 copa' AS nombre, '1 shot' AS nombre_en, 42.0 AS gramos, 'USDA FDC 171919: 1 Jigger 1.5 fl oz' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '1004' AS c, '1 copa' AS nombre, '1 shot' AS nombre_en, 42.0 AS gramos, 'USDA FDC 171919: 1 Jigger 1.5 fl oz' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '1002' AS c, '1 copa' AS nombre, '1 shot' AS nombre_en, 42.0 AS gramos, 'USDA FDC 171919: 1 Jigger 1.5 fl oz' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '1008' AS c, '1 copa' AS nombre, '1 shot' AS nombre_en, 42.0 AS gramos, 'USDA FDC 171919: 1 Jigger 1.5 fl oz' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'USDA' AS f, '173180' AS c, '1 cacito' AS nombre, '1 scoop' AS nombre_en, 32.0 AS gramos, 'USDA FDC 173180: 0.33 cup' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'USDA' AS f, '173177' AS c, '1 cacito' AS nombre, '1 scoop' AS nombre_en, 28.7 AS gramos, 'USDA FDC 173177: 3 scoop = 86 g' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'USDA' AS f, '174276' AS c, '1 cacito' AS nombre, '1 scoop' AS nombre_en, 28.4 AS gramos, 'USDA FDC 174276: 1 oz' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '11007' AS c, '1 sobre' AS nombre, '1 envelope' AS nombre_en, 7.0 AS gramos, 'USDA FDC 169599: 1 envelope (1 tbsp)' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '22004' AS c, '1 cucharada' AS nombre, '1 tablespoon' AS nombre_en, 7.0 AS gramos, 'USDA FDC 169599: 1 tbsp de polvo (estimación por la gelatina)' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '11009' AS c, '1 cucharada' AS nombre, '1 tablespoon' AS nombre_en, 8.0 AS gramos, 'USDA FDC 175043: 1 tbsp = 12 g de levadura seca' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '9660' AS c, '1 cucharada' AS nombre, '1 tablespoon' AS nombre_en, 7.0 AS gramos, 'USDA FDC 168892: 1 cup = 115 g / 16' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '31104' AS c, '1 barrita' AS nombre, '1 bar' AS nombre_en, 58.0 AS gramos, 'Barrita de 58 g, la habitual de las sustitutivas en España' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '4004' AS c, '1 bolsa pequeña' AS nombre, '1 small bag' AS nombre_en, 28.0 AS gramos, 'USDA FDC 169677: 1 oz' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '38417' AS c, '1 ración' AS nombre, '1 serving' AS nombre_en, 28.0 AS gramos, 'USDA FDC 167558: 1 oz' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '9231' AS c, '1 taza' AS nombre, '1 cup' AS nombre_en, 8.0 AS gramos, 'USDA FDC 167959: 1 cup' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '9230' AS c, '1 taza' AS nombre, '1 cup' AS nombre_en, 11.0 AS gramos, 'USDA FDC 167959: 1 cup = 8 g sin aceite' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '24001' AS c, '1 galleta' AS nombre, '1 biscuit' AS nombre_en, 6.0 AS gramos, 'Galleta María de 6 g, la habitual en España' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '24020' AS c, '1 galleta' AS nombre, '1 biscuit' AS nombre_en, 9.0 AS gramos, 'Galleta de 9 g, la habitual en los paquetes españoles' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '24036' AS c, '1 galleta' AS nombre, '1 biscuit' AS nombre_en, 14.0 AS gramos, 'USDA FDC 172717: 1 cookie = 14,2 g' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '24684' AS c, '1 galleta' AS nombre, '1 cookie' AS nombre_en, 14.2 AS gramos, 'USDA FDC 172717: 1 cookie' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '24632' AS c, '1 magdalena' AS nombre, '1 madeleine' AS nombre_en, 30.0 AS gramos, 'Magdalena de 30 g, la habitual en los paquetes españoles' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '23925' AS c, '1 porción' AS nombre, '1 slice' AS nombre_en, 63.0 AS gramos, 'USDA FDC 172707: 1 piece (1/12 of 10 inch cake)' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '23585' AS c, '1 porción' AS nombre, '1 slice' AS nombre_en, 63.0 AS gramos, 'USDA FDC 172707: 1 piece (1/12 of 10 inch cake)' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '23880' AS c, '1 donut' AS nombre, '1 doughnut' AS nombre_en, 45.0 AS gramos, 'USDA FDC 174992: 1 doughnut, medium' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '23799' AS c, '1 crepe' AS nombre, '1 crepe' AS nombre_en, 50.0 AS gramos, 'Crepe de 50 g, la habitual en los paquetes españoles' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '23850' AS c, '1 gofre' AS nombre, '1 waffle' AS nombre_en, 75.0 AS gramos, 'USDA FDC 175039: 1 waffle, round' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '31106' AS c, '1 barrita' AS nombre, '1 bar' AS nombre_en, 25.0 AS gramos, 'Barrita de 25 g, la habitual en España' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '31113' AS c, '1 barrita' AS nombre, '1 bar' AS nombre_en, 25.0 AS gramos, 'Barrita de 25 g, la habitual en España' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '31004' AS c, '1 onza' AS nombre, '1 square' AS nombre_en, 6.0 AS gramos, 'Tableta de 100 g en 16-17 onzas: 6 g' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '31004' AS c, '1 tableta' AS nombre, '1 bar' AS nombre_en, 100.0 AS gramos, 'Tableta de 100 g, la estándar en España' AS fuente_peso, 2 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '31005' AS c, '1 onza' AS nombre, '1 square' AS nombre_en, 6.0 AS gramos, 'Tableta de 100 g en 16-17 onzas: 6 g' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '31005' AS c, '1 tableta' AS nombre, '1 bar' AS nombre_en, 100.0 AS gramos, 'Tableta de 100 g, la estándar en España' AS fuente_peso, 2 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '31010' AS c, '1 onza' AS nombre, '1 square' AS nombre_en, 6.0 AS gramos, 'Tableta de 100 g en 16-17 onzas: 6 g' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '31032' AS c, '1 cucharada' AS nombre, '1 tablespoon' AS nombre_en, 15.0 AS gramos, 'Ración de 15 g declarada en los envases (USDA FDC 172470: 2 tbsp = 32 g de crema untable)' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '31000' AS c, '1 barrita' AS nombre, '1 bar' AS nombre_en, 44.0 AS gramos, 'USDA FDC 167587: 1 bar (1.55 oz)' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '31060' AS c, '10 gominolas' AS nombre, '10 gummies' AS nombre_en, 22.0 AS gramos, 'USDA FDC 167989: 10 gummy bears' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '39515' AS c, '1 bola' AS nombre, '1 scoop' AS nombre_en, 66.0 AS gramos, 'USDA FDC 167575: 1 serving 1/2 cup' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '39524' AS c, '1 bola' AS nombre, '1 scoop' AS nombre_en, 74.0 AS gramos, 'USDA FDC 167577: 0.5 cup' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '39526' AS c, '1 polo' AS nombre, '1 lolly' AS nombre_en, 66.0 AS gramos, 'USDA FDC 167577: 1 bar (2.75 fl oz)' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '31016' AS c, '1 cucharadita' AS nombre, '1 teaspoon' AS nombre_en, 4.2 AS gramos, 'USDA FDC 169655: 1 tsp' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '31016' AS c, '1 sobre' AS nombre, '1 packet' AS nombre_en, 8.0 AS gramos, 'Sobre de 8 g, el habitual en la hostelería española' AS fuente_peso, 2 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '31017' AS c, '1 cucharadita' AS nombre, '1 teaspoon' AS nombre_en, 4.2 AS gramos, 'USDA FDC 169655: 1 tsp' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '31008' AS c, '1 cucharada' AS nombre, '1 tablespoon' AS nombre_en, 21.0 AS gramos, 'USDA FDC 169640: 1 tbsp' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '31034' AS c, '1 cucharada' AS nombre, '1 tablespoon' AS nombre_en, 20.0 AS gramos, 'USDA FDC 169661: 1 tbsp' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '31024' AS c, '1 cucharada' AS nombre, '1 tablespoon' AS nombre_en, 20.0 AS gramos, 'USDA FDC 169641: 1 tbsp' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '30995' AS c, '1 cucharada' AS nombre, '1 tablespoon' AS nombre_en, 20.0 AS gramos, 'USDA FDC 169641: 1 tbsp' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '18100' AS c, '1 cucharada' AS nombre, '1 tablespoon' AS nombre_en, 5.4 AS gramos, 'USDA FDC 169593: 1 tbsp' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '11008' AS c, '1 cucharada' AS nombre, '1 tablespoon' AS nombre_en, 17.0 AS gramos, 'USDA FDC 168556: 1 tbsp' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '11013' AS c, '1 cucharadita' AS nombre, '1 teaspoon' AS nombre_en, 5.0 AS gramos, 'USDA FDC 172234: 1 tsp' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '11104' AS c, '1 cucharada' AS nombre, '1 tablespoon' AS nombre_en, 16.0 AS gramos, 'USDA FDC 174277: 1 tbsp' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '11018' AS c, '1 cucharada' AS nombre, '1 tablespoon' AS nombre_en, 15.0 AS gramos, 'USDA FDC 173469: 1 tbsp = 14,9 g' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '11091' AS c, '1 cucharada' AS nombre, '1 tablespoon' AS nombre_en, 16.0 AS gramos, 'USDA FDC 172241: 1 tbsp' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '11179' AS c, '1 cucharada' AS nombre, '1 tablespoon' AS nombre_en, 16.0 AS gramos, 'USDA FDC 171579: 0.25 cup = 63 g / 4' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '25525' AS c, 'Media taza' AS nombre, 'Half cup' AS nombre_en, 132.0 AS gramos, 'USDA FDC 171192: 1 serving 1/2 cup' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '11143' AS c, 'Media taza' AS nombre, 'Half cup' AS nombre_en, 125.0 AS gramos, 'USDA FDC 171192: 0.5 cup de salsa (estimación por volumen)' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '25620' AS c, '1 cucharada' AS nombre, '1 tablespoon' AS nombre_en, 15.0 AS gramos, 'USDA FDC 174289: 1 tbsp de crema untable (estimación por el hummus)' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '25947' AS c, '1 taza' AS nombre, '1 cup' AS nombre_en, 249.0 AS gramos, 'USDA FDC 174536: 1 cup' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '25031' AS c, '1 plato' AS nombre, '1 plate' AS nombre_en, 300.0 AS gramos, 'Ración de 300 g de plato único (estimación propia)' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '25404' AS c, '1 porción' AS nombre, '1 slice' AS nombre_en, 107.0 AS gramos, 'USDA FDC 173292: 1 slice' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '25404' AS c, '1 pizza mediana' AS nombre, '1 medium pizza' AS nombre_en, 400.0 AS gramos, 'Pizza congelada de 400 g, la más vendida en España' AS fuente_peso, 2 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '25435' AS c, '1 porción' AS nombre, '1 slice' AS nombre_en, 107.0 AS gramos, 'USDA FDC 173292: 1 slice' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '25435' AS c, '1 pizza mediana' AS nombre, '1 medium pizza' AS nombre_en, 400.0 AS gramos, 'Pizza congelada de 400 g, la más vendida en España' AS fuente_peso, 2 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '25081' AS c, '1 ración' AS nombre, '1 serving' AS nombre_en, 300.0 AS gramos, 'Bandeja individual de 300 g, la habitual en España' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '25085' AS c, '1 plato' AS nombre, '1 plate' AS nombre_en, 300.0 AS gramos, 'Ración de 300 g de plato único (estimación propia)' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '25135' AS c, '1 plato' AS nombre, '1 plate' AS nombre_en, 300.0 AS gramos, 'Ración de 300 g de plato único (estimación propia)' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '25413' AS c, '1 hamburguesa' AS nombre, '1 burger' AS nombre_en, 99.0 AS gramos, 'USDA FDC 170328: 1 sandwich' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '25429' AS c, '1 kebab' AS nombre, '1 kebab' AS nombre_en, 350.0 AS gramos, 'Kebab de 350 g (estimación propia)' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '25522' AS c, '1 bocadillo' AS nombre, '1 sandwich' AS nombre_en, 200.0 AS gramos, 'Media barra de 125 g con relleno (estimación propia)' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '4049' AS c, '1 croqueta' AS nombre, '1 croquette' AS nombre_en, 30.0 AS gramos, 'Croqueta de 30 g, la habitual congelada en España' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '25590' AS c, '1 falafel' AS nombre, '1 falafel' AS nombre_en, 17.0 AS gramos, 'USDA FDC 172455: 1 patty' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '25138' AS c, '1 plato' AS nombre, '1 plate' AS nombre_en, 300.0 AS gramos, 'Ración de 300 g de plato único (estimación propia)' AS fuente_peso, 1 AS orden
    UNION ALL
    SELECT 'CIQUAL' AS f, '11058' AS c, '1 pizca' AS nombre, '1 pinch' AS nombre_en, 0.4 AS gramos, 'USDA FDC 173468: 1 dash' AS fuente_peso, 1 AS orden
) r ON a.fuente = r.f AND a.codigo_origen = r.c
ON DUPLICATE KEY UPDATE alimento_raciones.gramos = alimento_raciones.gramos;
