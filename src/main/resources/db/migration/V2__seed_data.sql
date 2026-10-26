-- ============================================================================
-- The Unified Service Scheduler (USS) - Seed Dataset
-- Script: V2__seed_data.sql
-- Compliance: docs/rules/database-design-and-script-rules.md
-- Engine: PostgreSQL 17
-- Strategy: Natural code lookups for foreign keys; clean auto-incrementing IDs
-- ============================================================================

-- ----------------------------------------------------------------------------
-- Dealerships (Master Data)
-- ----------------------------------------------------------------------------
INSERT INTO dealerships (code, name, address, city, operating_open_time, operating_close_time, time_zone, is_active, version)
VALUES 
    ('DLR-METRO', 'Central Metro Dealership', '100 Grand Avenue', 'Metro City', '08:00:00', '18:00:00', 'UTC', TRUE, 0),
    ('DLR-WEST', 'Westside Auto Center', '450 Sunset Boulevard', 'West City', '08:30:00', '17:30:00', 'UTC', TRUE, 0);

-- ----------------------------------------------------------------------------
-- Service Types & Skill Requirements (Master Data)
-- ----------------------------------------------------------------------------
INSERT INTO service_types (code, name, description, standard_duration_minutes, required_skill_code, is_active, version)
VALUES 
    ('OIL_CHANGE', 'Synthetic Oil & Filter Service', 'Engine oil drainage, synthetic oil replacement, filter replacement, and multi-point check.', 30, 'GENERAL_MAINTENANCE', TRUE, 0),
    ('BRAKE_SERVICE', 'Comprehensive Brake System Service', 'Front & rear brake pad inspection, rotor resurfacing, brake fluid flush.', 60, 'BRAKE_SPECIALIST', TRUE, 0),
    ('EV_DIAGNOSTIC', 'EV High-Voltage Battery & Powertrain Diagnostic', 'Complete high-voltage state of health (SOH) scan, thermal management check, and firmware update.', 120, 'EV_CERTIFIED', TRUE, 0),
    ('MAJOR_SERVICE_30K', 'Major 30,000-Mile Composite Maintenance', 'Composite service combining spark plug inspection, fluid flushes, mechanical tuning, and chassis lubrication.', 180, 'ENGINE_DIAGNOSTIC', TRUE, 0);

-- ----------------------------------------------------------------------------
-- Service Bays (Central Metro Dealership: 5 Bays)
-- Reference dealership_id naturally via (SELECT id FROM dealerships WHERE code = 'DLR-METRO')
-- ----------------------------------------------------------------------------
INSERT INTO service_bays (dealership_id, bay_number, name, is_operational, version)
VALUES 
    ((SELECT id FROM dealerships WHERE code = 'DLR-METRO'), 1, 'Bay 1 - Express Lift', TRUE, 0),
    ((SELECT id FROM dealerships WHERE code = 'DLR-METRO'), 2, 'Bay 2 - General Stall', TRUE, 0),
    ((SELECT id FROM dealerships WHERE code = 'DLR-METRO'), 3, 'Bay 3 - Heavy Diagnostic Bay', TRUE, 0),
    ((SELECT id FROM dealerships WHERE code = 'DLR-METRO'), 4, 'Bay 4 - EV Insulated Stall', TRUE, 0),
    ((SELECT id FROM dealerships WHERE code = 'DLR-METRO'), 5, 'Bay 5 - Alignment & Brake Rack', TRUE, 0);

-- ----------------------------------------------------------------------------
-- Technicians (Central Metro Dealership: 5 Technicians)
-- ----------------------------------------------------------------------------
INSERT INTO technicians (dealership_id, employee_code, first_name, last_name, email, is_active, version)
VALUES 
    ((SELECT id FROM dealerships WHERE code = 'DLR-METRO'), 'TECH-001', 'Alex', 'Nguyen', 'alex.nguyen@dealership.com', TRUE, 0),
    ((SELECT id FROM dealerships WHERE code = 'DLR-METRO'), 'TECH-002', 'David', 'Tran', 'david.tran@dealership.com', TRUE, 0),
    ((SELECT id FROM dealerships WHERE code = 'DLR-METRO'), 'TECH-003', 'Elena', 'Gomez', 'elena.gomez@dealership.com', TRUE, 0),
    ((SELECT id FROM dealerships WHERE code = 'DLR-METRO'), 'TECH-004', 'Marcus', 'Lee', 'marcus.lee@dealership.com', TRUE, 0),
    ((SELECT id FROM dealerships WHERE code = 'DLR-METRO'), 'TECH-005', 'Sarah', 'Jenkins', 'sarah.jenkins@dealership.com', TRUE, 0);

-- ----------------------------------------------------------------------------
-- Technician Skills Mapping
-- Reference technician_id naturally via (SELECT id FROM technicians WHERE employee_code = ...)
-- ----------------------------------------------------------------------------
INSERT INTO technician_skills (technician_id, skill_code, certified_date)
VALUES 
    -- Alex Nguyen: General Maintenance & Brake Specialist
    ((SELECT id FROM technicians WHERE employee_code = 'TECH-001'), 'GENERAL_MAINTENANCE', '2023-01-15'),
    ((SELECT id FROM technicians WHERE employee_code = 'TECH-001'), 'BRAKE_SPECIALIST', '2023-06-20'),
    
    -- David Tran: Brake Specialist & Engine Diagnostic
    ((SELECT id FROM technicians WHERE employee_code = 'TECH-002'), 'BRAKE_SPECIALIST', '2022-03-10'),
    ((SELECT id FROM technicians WHERE employee_code = 'TECH-002'), 'ENGINE_DIAGNOSTIC', '2022-11-05'),
    
    -- Elena Gomez: EV Certified & Engine Diagnostic
    ((SELECT id FROM technicians WHERE employee_code = 'TECH-003'), 'EV_CERTIFIED', '2024-02-01'),
    ((SELECT id FROM technicians WHERE employee_code = 'TECH-003'), 'ENGINE_DIAGNOSTIC', '2023-08-15'),
    
    -- Marcus Lee: General Maintenance
    ((SELECT id FROM technicians WHERE employee_code = 'TECH-004'), 'GENERAL_MAINTENANCE', '2023-05-12'),
    
    -- Sarah Jenkins: EV Certified & General Maintenance
    ((SELECT id FROM technicians WHERE employee_code = 'TECH-005'), 'EV_CERTIFIED', '2024-01-10'),
    ((SELECT id FROM technicians WHERE employee_code = 'TECH-005'), 'GENERAL_MAINTENANCE', '2023-09-01');
