-- Migration: Add is_public column to monitors table
-- Run this against each org schema that already exists
ALTER TABLE monitors ADD COLUMN IF NOT EXISTS is_public BOOLEAN NOT NULL DEFAULT FALSE;
