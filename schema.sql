CREATE DATABASE DaVinciPriorAuthDB;
GO

USE DaVinciPriorAuthDB;
GO

CREATE TABLE PriorAuthAuditLog (
    AuditId INT IDENTITY(1,1) PRIMARY KEY,
    PlacerOrderNumber VARCHAR(50) NOT NULL,
    PatientReference VARCHAR(100),
    ServiceRequestReference VARCHAR(100),
    CptCode VARCHAR(20),
    IcdCode VARCHAR(20),
    CrdDecision VARCHAR(50),
    PasDisposition VARCHAR(200),
    AuthorizationNumber VARCHAR(50),
    CreatedAt DATETIME2 DEFAULT GETDATE()
);
GO 
use DaVinciPriorAuthDB;
SELECT * FROM DaVinciPriorAuthDB.dbo.PriorAuthAuditLog;