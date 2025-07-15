-- Clean up test data for vocabulary synchronizer tests

-- Remove test dataset (this will also clean up the category array)
DELETE FROM dataset 
WHERE key = '38f06820-08c5-42b2-94f6-47cc3e83a54a';

-- Remove test installation
DELETE FROM installation 
WHERE key = '1e9136f0-78fd-40cd-8b25-26c78a376d8d';

-- Remove test organization
DELETE FROM organization 
WHERE key = 'ff593857-44c2-4011-be20-8403e8d0bd9a';

-- Remove test node
DELETE FROM node 
WHERE key = 'a49e75d9-7b07-4d01-9be8-6ab2133f42f9'; 