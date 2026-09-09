package com.marketfastroute.shared.web;

import com.marketfastroute.map.MapConsistencyException;
import com.marketfastroute.map.StoreMapNotFoundException;
import com.marketfastroute.product.ProductNotFoundException;
import com.marketfastroute.product.ProductLocationConsistencyException;
import com.marketfastroute.product.ProductLocationNotFoundException;
import com.marketfastroute.store.StoreNotFoundException;
import com.marketfastroute.routing.InvalidRouteRequestException;
import com.marketfastroute.routing.RouteConfigurationException;
import com.marketfastroute.routing.RoutePathNotFoundException;
import com.marketfastroute.routing.RoutePointNotFoundException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

	@ExceptionHandler(StoreNotFoundException.class)
	public ResponseEntity<ApiErrorResponse> handleStoreNotFound(StoreNotFoundException exception) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND)
				.body(new ApiErrorResponse(
						"STORE_NOT_FOUND",
						"Store not found",
						Map.of()
				));
	}

	@ExceptionHandler(StoreMapNotFoundException.class)
	public ResponseEntity<ApiErrorResponse> handleStoreMapNotFound(StoreMapNotFoundException exception) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND)
				.body(new ApiErrorResponse(
						"STORE_MAP_NOT_FOUND",
						"Active map not found for store",
						Map.of()
				));
	}

	@ExceptionHandler(ProductNotFoundException.class)
	public ResponseEntity<ApiErrorResponse> handleProductNotFound(ProductNotFoundException exception) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND)
				.body(new ApiErrorResponse(
						"PRODUCT_NOT_FOUND",
						"Product not found in store",
						Map.of()
				));
	}

	@ExceptionHandler(ProductLocationNotFoundException.class)
	public ResponseEntity<ApiErrorResponse> handleProductLocationNotFound(ProductLocationNotFoundException exception) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND)
				.body(new ApiErrorResponse(
						"PRODUCT_LOCATION_NOT_FOUND",
						"Product location not found",
						Map.of()
				));
	}

	@ExceptionHandler(ProductLocationConsistencyException.class)
	public ResponseEntity<ApiErrorResponse> handleProductLocationConsistency(
			ProductLocationConsistencyException exception
	) {
		log.error("Product location data is inconsistent", exception);

		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(new ApiErrorResponse(
						"PRODUCT_LOCATION_INCONSISTENT",
						"Product location data is inconsistent",
						Map.of()
				));
	}

	@ExceptionHandler(MapConsistencyException.class)
	public ResponseEntity<ApiErrorResponse> handleMapConsistency(MapConsistencyException exception) {
		log.error("Map data is inconsistent", exception);

		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(new ApiErrorResponse(
						"MAP_INCONSISTENT",
						"Map data is inconsistent",
						Map.of()
				));
	}

	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	public ResponseEntity<ApiErrorResponse> handleInvalidParameter(MethodArgumentTypeMismatchException exception) {
		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
				.body(new ApiErrorResponse(
						"INVALID_PARAMETER",
						"Invalid request parameter",
						Map.of()
				));
	}

	@ExceptionHandler({
			InvalidRouteRequestException.class,
			MethodArgumentNotValidException.class,
			HttpMessageNotReadableException.class
	})
	public ResponseEntity<ApiErrorResponse> handleInvalidRequest(Exception exception) {
		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
				.body(new ApiErrorResponse(
						"INVALID_REQUEST",
						"Invalid request",
						Map.of()
				));
	}

	@ExceptionHandler(RoutePointNotFoundException.class)
	public ResponseEntity<ApiErrorResponse> handleRoutePointNotFound(RoutePointNotFoundException exception) {
		String code = "checkout".equals(exception.getPointType())
				? "ROUTE_CHECKOUT_NOT_FOUND"
				: "ROUTE_ENTRY_NOT_FOUND";
		return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
				.body(new ApiErrorResponse(code, "Route endpoint is not configured", Map.of()));
	}

	@ExceptionHandler(RoutePathNotFoundException.class)
	public ResponseEntity<ApiErrorResponse> handleRoutePathNotFound(RoutePathNotFoundException exception) {
		return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
				.body(new ApiErrorResponse(
						"ROUTE_PATH_NOT_FOUND",
						"No path is available for the requested route",
						Map.of()
				));
	}

	@ExceptionHandler(RouteConfigurationException.class)
	public ResponseEntity<ApiErrorResponse> handleRouteConfiguration(RouteConfigurationException exception) {
		return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
				.body(new ApiErrorResponse(
						"ROUTE_CONFIGURATION_INVALID",
						"Route configuration is ambiguous",
						Map.of()
				));
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiErrorResponse> handleUnexpectedException(Exception exception) {
		log.error("Unhandled request error", exception);

		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(new ApiErrorResponse(
						"INTERNAL_SERVER_ERROR",
						"An unexpected error occurred",
						Map.of()
				));
	}
}
