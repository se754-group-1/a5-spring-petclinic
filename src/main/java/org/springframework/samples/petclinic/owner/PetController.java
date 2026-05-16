/*
 * Copyright 2012-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.samples.petclinic.owner;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;

import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import jakarta.validation.Valid;

import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Controller responsible for handling pet-related requests for a specific owner,
 * including creating, editing, validating, and saving pet records.
 *
 * @author Juergen Hoeller
 * @author Ken Krebs
 * @author Arjen Poutsma
 * @author Wick Dynex
 */
@Controller
@RequestMapping("/owners/{ownerId}")
class PetController {

	private static final String VIEWS_PETS_CREATE_OR_UPDATE_FORM = "pets/createOrUpdatePetForm";

	private final OwnerRepository owners;

	private final PetTypeRepository types;

	/**
	 * Creates a new controller for managing pet-related requests.
	 * @param owners the repository used to access and persist owner records and their
	 * pets
	 * @param types the repository used to access available pet types
	 */
	public PetController(OwnerRepository owners, PetTypeRepository types) {
		this.owners = owners;
		this.types = types;
	}

	/**
	 * Adds all available pet types to the model for use in pet creation and update forms.
	 * @return a collection of available pet types
	 */
	@ModelAttribute("types")
	public Collection<PetType> populatePetTypes() {
		return this.types.findPetTypes();
	}

	/**
	 * Finds the owner associated with the current request path.
	 * @param ownerId the identifier of the owner to retrieve
	 * @return the owner matching the supplied identifier
	 * @throws IllegalArgumentException if no owner exists for the supplied identifier
	 */
	@ModelAttribute("owner")
	public Owner findOwner(@PathVariable("ownerId") int ownerId) {
		Optional<Owner> optionalOwner = this.owners.findById(ownerId);
		return optionalOwner.orElseThrow(() -> ownerNotFoundException(ownerId));
	}

	/**
	 * Finds the pet associated with the current request path, or creates a new pet when
	 * no pet identifier is supplied.
	 * @param ownerId the identifier of the owner who owns the pet
	 * @param petId the optional identifier of the pet to retrieve
	 * @return the existing pet matching the supplied pet identifier, or a new pet when
	 * {@code petId} is {@code null}
	 * @throws IllegalArgumentException if no owner exists for the supplied owner
	 * identifier
	 */
	@ModelAttribute("pet")
	public Pet findPet(@PathVariable("ownerId") int ownerId,
			@PathVariable(name = "petId", required = false) Integer petId) {

		if (petId == null) {
			return new Pet();
		}

		Optional<Owner> optionalOwner = this.owners.findById(ownerId);
		Owner owner = optionalOwner.orElseThrow(() -> ownerNotFoundException(ownerId));
		return owner.getPet(petId);
	}

	/**
	 * Configures the owner binder to prevent clients from binding owner identifier fields
	 * directly.
	 * @param dataBinder the binder used by Spring MVC to bind request parameters to owner
	 * objects
	 */
	@InitBinder("owner")
	public void initOwnerBinder(WebDataBinder dataBinder) {
		dataBinder.setDisallowedFields("id", "*.id");
	}

	/**
	 * Configures the pet binder with the pet validator and prevents clients from binding
	 * pet identifier fields directly.
	 * @param dataBinder the binder used by Spring MVC to bind request parameters to pet
	 * objects
	 */
	@InitBinder("pet")
	public void initPetBinder(WebDataBinder dataBinder) {
		dataBinder.setValidator(new PetValidator());
		dataBinder.setDisallowedFields("id", "*.id");
	}

	/**
	 * Displays the form used to create a new pet for the specified owner.
	 * @param owner the owner to whom the new pet will belong
	 * @param model the model used to expose form data to the view
	 * @return the name of the pet creation form view
	 */
	@GetMapping("/pets/new")
	public String initCreationForm(Owner owner, ModelMap model) {
		Pet pet = new Pet();
		owner.addPet(pet);
		return VIEWS_PETS_CREATE_OR_UPDATE_FORM;
	}

	/**
	 * Processes the submitted form for creating a new pet.
	 * @param owner the owner to whom the pet will be added
	 * @param pet the pet populated from the submitted form data
	 * @param result the binding result containing validation errors, if any
	 * @param redirectAttributes attributes used to pass flash messages after a redirect
	 * @return the pet creation form view when validation fails, or a redirect to the
	 * owner's details page when the pet is created successfully
	 */
	@PostMapping("/pets/new")
	public String processCreationForm(Owner owner, @Valid Pet pet, BindingResult result,
			RedirectAttributes redirectAttributes) {

		if (StringUtils.hasText(pet.getName()) && pet.isNew() && owner.getPet(pet.getName(), true) != null) {
			result.rejectValue("name", "duplicate", "already exists");
		}

		LocalDate currentDate = LocalDate.now();
		if (pet.getBirthDate() != null && pet.getBirthDate().isAfter(currentDate)) {
			result.rejectValue("birthDate", "typeMismatch.birthDate");
		}

		if (result.hasErrors()) {
			return VIEWS_PETS_CREATE_OR_UPDATE_FORM;
		}

		owner.addPet(pet);
		this.owners.save(owner);
		redirectAttributes.addFlashAttribute("message", "New Pet has been Added");
		return "redirect:/owners/{ownerId}";
	}

	/**
	 * Displays the form used to edit an existing pet.
	 * @return the name of the pet update form view
	 */
	@GetMapping("/pets/{petId}/edit")
	public String initUpdateForm() {
		return VIEWS_PETS_CREATE_OR_UPDATE_FORM;
	}

	/**
	 * Processes the submitted form for updating an existing pet.
	 * @param owner the owner who owns the pet being updated
	 * @param pet the pet populated from the submitted form data
	 * @param result the binding result containing validation errors, if any
	 * @param redirectAttributes attributes used to pass flash messages after a redirect
	 * @return the pet update form view when validation fails, or a redirect to the
	 * owner's details page when the pet is updated successfully
	 */
	@PostMapping("/pets/{petId}/edit")
	public String processUpdateForm(Owner owner, @Valid Pet pet, BindingResult result,
			RedirectAttributes redirectAttributes) {

		String petName = pet.getName();

		// checking if the pet name already exists for the owner
		if (StringUtils.hasText(petName)) {
			Pet existingPet = owner.getPet(petName, false);
			if (existingPet != null && !Objects.equals(existingPet.getId(), pet.getId())) {
				result.rejectValue("name", "duplicate", "already exists");
			}
		}

		LocalDate currentDate = LocalDate.now();
		if (pet.getBirthDate() != null && pet.getBirthDate().isAfter(currentDate)) {
			result.rejectValue("birthDate", "typeMismatch.birthDate");
		}

		if (result.hasErrors()) {
			return VIEWS_PETS_CREATE_OR_UPDATE_FORM;
		}

		updatePetDetails(owner, pet);
		redirectAttributes.addFlashAttribute("message", "Pet details has been edited");
		return "redirect:/owners/{ownerId}";
	}

	private IllegalArgumentException ownerNotFoundException(int ownerId) {
		return new IllegalArgumentException(
				"Owner not found with id: " + ownerId + ". Please ensure the ID is correct");
	}

	/**
	 * Updates the pet details if it exists or adds a new pet to the owner.
	 * @param owner the owner of the pet
	 * @param pet the pet containing the updated details
	 */
	private void updatePetDetails(Owner owner, Pet pet) {
		Integer id = pet.getId();
		Assert.state(id != null, "'pet.getId()' must not be null");
		Pet existingPet = owner.getPet(id);
		if (existingPet != null) {
			// Update existing pet's properties
			existingPet.setName(pet.getName());
			existingPet.setBirthDate(pet.getBirthDate());
			existingPet.setType(pet.getType());
		}
		else {
			owner.addPet(pet);
		}
		this.owners.save(owner);
	}

}