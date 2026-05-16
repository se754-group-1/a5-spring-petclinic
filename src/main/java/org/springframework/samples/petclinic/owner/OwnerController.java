/*
 * Copyright 2012-2025 the original author or authors.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *      https://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.samples.petclinic.owner;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.samples.petclinic.system.PetClinicException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import jakarta.validation.Valid;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Controller responsible for handling owner-related web requests, including creating,
 * searching, updating, and displaying owner records.
 *
 * @author Juergen Hoeller
 * @author Ken Krebs
 * @author Arjen Poutsma
 * @author Michael Isvy
 * @author Wick Dynex
 */
@Controller
class OwnerController {

	private static final String VIEWS_OWNER_CREATE_OR_UPDATE_FORM = "owners/createOrUpdateOwnerForm";

	private static final String ERROR_ATTRIBUTE = "error";

	private final OwnerRepository owners;

	/**
	 * Creates a new controller for managing owner-related requests.
	 * @param owners the repository used to access and persist owner records
	 */
	public OwnerController(OwnerRepository owners) {
		this.owners = owners;
	}

	/**
	 * Configures the web data binder to prevent clients from binding owner identifier
	 * fields directly.
	 * @param dataBinder the binder used by Spring MVC to bind request parameters to model
	 * objects
	 */
	@InitBinder
	public void setAllowedFields(WebDataBinder dataBinder) {
		dataBinder.setDisallowedFields("id", "*.id");
	}

	/**
	 * Finds the owner associated with the supplied owner identifier, or creates a new
	 * owner when no identifier is supplied.
	 * @param ownerId the optional identifier of the owner to retrieve
	 * @return the existing owner matching the identifier, or a new owner when
	 * {@code ownerId} is {@code null}
	 * @throws PetClinicException if an owner identifier is supplied but no matching owner
	 * exists
	 */
	@ModelAttribute("owner")
	public Owner findOwner(@PathVariable(name = "ownerId", required = false) Integer ownerId) {
		return ownerId == null ? new Owner()
				: this.owners.findById(ownerId).orElseThrow(() -> ownerNotFoundException(ownerId));
	}

	/**
	 * Displays the form used to create a new owner.
	 * @return the name of the owner creation form view
	 */
	@GetMapping("/owners/new")
	public String initCreationForm() {
		return VIEWS_OWNER_CREATE_OR_UPDATE_FORM;
	}

	/**
	 * Processes the submitted form for creating a new owner.
	 * @param owner the owner populated from the submitted form data
	 * @param result the binding result containing validation errors, if any
	 * @param redirectAttributes attributes used to pass flash messages after a redirect
	 * @return the owner creation form view if validation fails, or a redirect to the
	 * newly created owner's details page if successful
	 */
	@PostMapping("/owners/new")
	public String processCreationForm(@Valid Owner owner, BindingResult result, RedirectAttributes redirectAttributes) {
		if (result.hasErrors()) {
			redirectAttributes.addFlashAttribute(ERROR_ATTRIBUTE, "There was an error in creating the owner.");
			return VIEWS_OWNER_CREATE_OR_UPDATE_FORM;
		}
		this.owners.save(owner);
		redirectAttributes.addFlashAttribute("message", "New Owner Created");
		return "redirect:/owners/" + owner.getId();
	}

	/**
	 * Displays the form used to search for owners.
	 * @return the name of the owner search form view
	 */
	@GetMapping("/owners/find")
	public String initFindForm() {
		return "owners/findOwners";
	}

	/**
	 * Processes the owner search form and displays matching owner records.
	 * @param page the page number of results to display
	 * @param owner the owner object containing the submitted search criteria
	 * @param result the binding result used to report search errors
	 * @param model the model used to expose search results and pagination data to the
	 * view
	 * @return the owner search view when no results are found, a redirect to the owner
	 * details page when exactly one result is found, or the paginated owner list view
	 * when multiple results are found
	 */
	@GetMapping("/owners")
	public String processFindForm(@RequestParam(defaultValue = "1") int page, Owner owner, BindingResult result,
			Model model) {
		String lastName = owner.getLastName();
		if (lastName == null) {
			lastName = "";
		}
		Page<Owner> ownersResults = findPaginatedForOwnersLastName(page, lastName);
		if (ownersResults.isEmpty()) {
			result.rejectValue("lastName", "notFound", "not found");
			return "owners/findOwners";
		}
		if (ownersResults.getTotalElements() == 1) {
			owner = ownersResults.iterator().next();
			return "redirect:/owners/" + owner.getId();
		}
		return addPaginationModel(page, model, ownersResults);
	}

	private String addPaginationModel(int page, Model model, Page<Owner> paginated) {
		List<Owner> listOwners = paginated.getContent();
		model.addAttribute("currentPage", page);
		model.addAttribute("totalPages", paginated.getTotalPages());
		model.addAttribute("totalItems", paginated.getTotalElements());
		model.addAttribute("listOwners", listOwners);
		return "owners/ownersList";
	}

	private Page<Owner> findPaginatedForOwnersLastName(int page, String lastname) {
		int pageSize = 5;
		Pageable pageable = PageRequest.of(page - 1, pageSize);
		return owners.findByLastNameStartingWith(lastname, pageable);
	}

	/**
	 * Displays the form used to edit an existing owner.
	 * @return the name of the owner update form view
	 */
	@GetMapping("/owners/{ownerId}/edit")
	public String initUpdateOwnerForm() {
		return VIEWS_OWNER_CREATE_OR_UPDATE_FORM;
	}

	/**
	 * Processes the submitted form for updating an existing owner.
	 * @param owner the owner populated from the submitted form data
	 * @param result the binding result containing validation errors, if any
	 * @param ownerId the identifier of the owner being updated
	 * @param redirectAttributes attributes used to pass flash messages after a redirect
	 * @return the owner update form view when validation fails, a redirect back to the
	 * edit page when the owner identifier does not match, or a redirect to the owner
	 * details page when the update succeeds
	 */
	@PostMapping("/owners/{ownerId}/edit")
	public String processUpdateOwnerForm(@Valid Owner owner, BindingResult result, @PathVariable("ownerId") int ownerId,
			RedirectAttributes redirectAttributes) {
		if (result.hasErrors()) {
			redirectAttributes.addFlashAttribute(ERROR_ATTRIBUTE, "There was an error in updating the owner.");
			return VIEWS_OWNER_CREATE_OR_UPDATE_FORM;
		}
		if (!Objects.equals(owner.getId(), ownerId)) {
			result.rejectValue("id", "mismatch", "The owner ID in the form does not match the URL.");
			redirectAttributes.addFlashAttribute(ERROR_ATTRIBUTE, "Owner ID mismatch. Please try again.");
			return "redirect:/owners/{ownerId}/edit";
		}
		owner.setId(ownerId);
		this.owners.save(owner);
		redirectAttributes.addFlashAttribute("message", "Owner Values Updated");
		return "redirect:/owners/{ownerId}";
	}

	/**
	 * Displays the details page for a specific owner.
	 * @param ownerId the identifier of the owner to display
	 * @return a model and view containing the selected owner and the owner details view
	 * @throws PetClinicException if no owner exists for the supplied identifier
	 */
	@GetMapping("/owners/{ownerId}")
	public ModelAndView showOwner(@PathVariable("ownerId") int ownerId) {
		ModelAndView mav = new ModelAndView("owners/ownerDetails");
		Optional<Owner> optionalOwner = this.owners.findById(ownerId);
		Owner owner = optionalOwner.orElseThrow(() -> ownerNotFoundException(ownerId));
		mav.addObject(owner);
		return mav;
	}

	/**
	 * Builds a PetClinicException for an owner that was not found.
	 * @param ownerId the owner identifier that was looked up
	 * @return a PetClinicException with a descriptive message
	 */
	private PetClinicException ownerNotFoundException(int ownerId) {
		return new PetClinicException("Owner not found with id: " + ownerId + ". Please ensure the ID is correct.");
	}

}