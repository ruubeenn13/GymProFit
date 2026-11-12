package com.gymprofit.api.service.alta;

import com.gymprofit.api.enums.PasoAlta;
import com.gymprofit.api.repository.jpa.IPasoAltaDiaRepository;
import com.gymprofit.api.service.admin.AdminResumenService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

// ============================================================
// EmbudoAltaService — suma uno al paso del alta del día (GP-019, DEC-064)
//
// No recibe la petición ni el usuario: solo el paso. Así no hay forma de que algo de
// quien lo manda acabe en la fila. El día es el de Madrid, como el resto del panel.
// ============================================================
@Service
@RequiredArgsConstructor
public class EmbudoAltaService {

    private final IPasoAltaDiaRepository repositorio;

    /**
     * Cuenta una llegada a {@code paso} en el día de hoy de Madrid.
     *
     * @param paso la pantalla del alta
     */
    @Transactional
    public void contar(PasoAlta paso) {
        repositorio.sumar(LocalDate.now(AdminResumenService.MADRID), paso.name());
    }
}
