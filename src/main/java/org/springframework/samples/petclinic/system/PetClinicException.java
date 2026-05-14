package org.springframework.samples.petclinic.system;

/**
 * Custom runtime exception for the PetClinic application. Replaces generic
 * {@link RuntimeException} so that error handling can be explicit and traceable, reducing
 * cognitive complexity during debugging and maintenance.
 */
public class PetClinicException extends RuntimeException {

	public PetClinicException(String message) {
		super(message);
	}

	public PetClinicException(String message, Throwable cause) {
		super(message, cause);
	}

}