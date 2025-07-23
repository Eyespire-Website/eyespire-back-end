package org.eyespire.eyespireapi.repository;

import org.eyespire.eyespireapi.dto.InvoiceCreationRequestDTO;
import org.eyespire.eyespireapi.model.AppointmentInvoice;
import org.eyespire.eyespireapi.model.enums.PrescriptionStatus;

import java.util.List;
import java.util.Optional;

public interface AppointmentInvoiceRepositoryCustom {
    AppointmentInvoice createInvoice(Integer appointmentId, List<Integer> serviceIds, 
                                   List<InvoiceCreationRequestDTO.MedicationDTO> medications);
    
    Optional<AppointmentInvoice> updatePrescriptionStatus(Integer appointmentId, PrescriptionStatus status);
}
