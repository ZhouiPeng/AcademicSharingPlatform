SHELL := cmd
MVN := mvn
SERVICES := achievement-service admin-service analytics-service data-sync-service file-service gateway-service user-service

.PHONY: all help build-all clean-all

all: build-all

help:
	@echo Usage: make [target]
	@echo.
	@echo Targets:
	@echo   build-all        Build all services
	@echo   clean-all        Clean all services
	@echo   build-service    Build by specifying SERVICE=name
	@echo   clean-service    Clean by specifying SERVICE=name

build-all:
	@echo ========================================
	@echo Building all services
	@echo ========================================
	@for %%s in ($(SERVICES)) do ( \
		echo. && \
		echo ======================================== && \
		echo Building service: %%s && \
		echo ======================================== && \
		cd services\%%s && \
		mvn clean package -DskipTests && \
		cd ..\.. && \
		echo [OK] Service %%s built successfully || ( \
			echo [ERROR] Failed to build %%s && \
			exit /b 1 \
		) \
	)
	@echo.
	@echo ========================================
	@echo All services built successfully!
	@echo ========================================

clean-all:
	@echo ========================================
	@echo Cleaning all services
	@echo ========================================
	@for %%s in ($(SERVICES)) do ( \
		echo. && \
		echo ======================================== && \
		echo Cleaning service: %%s && \
		echo ======================================== && \
		cd services\%%s && \
		mvn clean && \
		cd ..\.. && \
		echo [OK] Service %%s cleaned \
	)

build-service:
	@if "$(SERVICE)"=="" ( \
		echo ERROR: Please specify SERVICE=service-name && \
		echo Usage: make SERVICE=admin-service build-service && \
		exit /b 1 \
	)
	@if not exist "services\$(SERVICE)" ( \
		echo ERROR: Service '$(SERVICE)' not found && \
		echo Available services: $(SERVICES) && \
		exit /b 1 \
	)
	@echo ========================================
	@echo Building service: $(SERVICE)
	@echo ========================================
	cd services\$(SERVICE) && mvn clean package -DskipTests
	@echo [OK] Service $(SERVICE) built successfully

clean-service:
	@if "$(SERVICE)"=="" ( \
		echo ERROR: Please specify SERVICE=service-name && \
		echo Usage: make SERVICE=admin-service clean-service && \
		exit /b 1 \
	)
	@if not exist "services\$(SERVICE)" ( \
		echo ERROR: Service '$(SERVICE)' not found && \
		echo Available services: $(SERVICES) && \
		exit /b 1 \
	)
	@echo ========================================
	@echo Cleaning service: $(SERVICE)
	@echo ========================================
	cd services\$(SERVICE) && mvn clean
	@echo [OK] Service $(SERVICE) cleaned