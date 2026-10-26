-- ============================================================================
-- The Unified Service Scheduler (USS) - Seed Dataset for Westside Dealership
-- Script: V3__seed_westside_dealership.sql
-- Purpose: Ensure both dealerships have operational bays and qualified technicians
-- ============================================================================

-- ----------------------------------------------------------------------------
-- Service Bays (Westside Auto Center: 3 Bays)
-- ----------------------------------------------------------------------------
INSERT INTO service_bays (dealership_id, bay_number, name, is_operational, version)
VALUES 
    ((SELECT id FROM dealerships WHERE code = 'DLR-WEST'), 1, 'West Bay 1 - Quick Service', TRUE, 0),
    ((SELECT id FROM dealerships WHERE code = 'DLR-WEST'), 2, 'West Bay 2 - General Stall', TRUE, 0),
    ((SELECT id FROM dealerships WHERE code = 'DLR-WEST'), 3, 'West Bay 3 - Multi-Service Rack', TRUE, 0);

-- ----------------------------------------------------------------------------
-- Technicians (Westside Auto Center: 3 Technicians)
-- ----------------------------------------------------------------------------
INSERT INTO technicians (dealership_id, employee_code, first_name, last_name, email, is_active, version)
VALUES 
    ((SELECT id FROM dealerships WHERE code = 'DLR-WEST'), 'TECH-101', 'Brian', 'Miller', 'brian.miller@westside.com', TRUE, 0),
    ((SELECT id FROM dealerships WHERE code = 'DLR-WEST'), 'TECH-102', 'Chloe', 'Davis', 'chloe.davis@westside.com', TRUE, 0),
    ((SELECT id FROM dealerships WHERE code = 'DLR-WEST'), 'TECH-103', 'Ethan', 'Wilson', 'ethan.wilson@westside.com', TRUE, 0);

-- ----------------------------------------------------------------------------
-- Technician Skills Mapping (Westside Technicians)
-- ----------------------------------------------------------------------------
INSERT INTO technician_skills (technician_id, skill_code, certified_date)
VALUES 
    -- Brian Miller: General Maintenance & Brake Specialist
    ((SELECT id FROM technicians WHERE employee_code = 'TECH-101'), 'GENERAL_MAINTENANCE', '2023-02-15'),
    ((SELECT id FROM technicians WHERE employee_code = 'TECH-101'), 'BRAKE_SPECIALIST', '2023-07-20'),
    
    -- Chloe Davis: EV Certified & Brake Specialist
    ((SELECT id FROM technicians WHERE employee_code = 'TECH-102'), 'EV_CERTIFIED', '2024-03-01'),
    ((SELECT id FROM technicians WHERE employee_code = 'TECH-102'), 'BRAKE_SPECIALIST', '2023-11-10'),
    
    -- Ethan Wilson: Engine Diagnostic & General Maintenance
    ((SELECT id FROM technicians WHERE employee_code = 'TECH-103'), 'ENGINE_DIAGNOSTIC', '2023-04-18'),
    ((SELECT id FROM technicians WHERE employee_code = 'TECH-103'), 'GENERAL_MAINTENANCE', '2023-10-05');
