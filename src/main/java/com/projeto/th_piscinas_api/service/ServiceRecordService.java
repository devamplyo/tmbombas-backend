package com.projeto.th_piscinas_api.service;

import com.projeto.th_piscinas_api.dto.stored.ServiceRecordPhotoResponse;
import com.projeto.th_piscinas_api.dto.stored.ServiceRecordResponse;
import com.projeto.th_piscinas_api.exception.EmptyFileException;
import com.projeto.th_piscinas_api.exception.OrderNotFoundException;
import com.projeto.th_piscinas_api.exception.ProfileNotValidateException;
import com.projeto.th_piscinas_api.model.ServiceOrder;
import com.projeto.th_piscinas_api.model.ServiceRecord;
import com.projeto.th_piscinas_api.model.ServiceRecordPhoto;
import com.projeto.th_piscinas_api.model.User;
import com.projeto.th_piscinas_api.repository.ServiceOrderRepository;
import com.projeto.th_piscinas_api.repository.ServiceRecordRepository;
import com.projeto.th_piscinas_api.util.Perfil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.List;

@RequiredArgsConstructor
@Service
public class ServiceRecordService {

    private final ServiceRecordRepository recordRepository;
    private final ServiceOrderRepository serviceOrderRepository;
    private final ImageStoreService imageStoreService;

    @Transactional
    public ServiceRecordResponse addRecord(Long orderId, String note,
                                           List<MultipartFile> photos, User technician)
            throws IOException, IOException {

        ServiceOrder order = serviceOrderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(
                        "Ordem de serviço não encontrada: " + orderId));

        // only the service order's technician can record it
        if (order.getTechnician() == null
                || !order.getTechnician().getId().equals(technician.getId())) {
            throw new ProfileNotValidateException("Este serviço não está atribuído a você");
        }

        boolean semTexto = note == null || note.isBlank();
        boolean semFoto = photos == null || photos.isEmpty();
        if (semTexto && semFoto) {
            throw new EmptyFileException("Informe um texto ou ao menos uma foto");
        }

        ServiceRecord record = ServiceRecord.builder()
                .serviceOrder(order)
                .authorId(technician.getId())
                .note(note)
                .build();

        if (photos != null) {
            // teste de robustez: era RuntimeException cru (virava 500). Agora
            // 400 limpo, tratado nativamente pelo Spring.
            if (photos.size() > 6) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Máximo de 6 fotos permitido");
            }
            for (MultipartFile file : photos) {
                // teste de robustez: o upload aceitava QUALQUER arquivo (HTML/SVG
                // etc.), servido depois pelo domínio de CDN. Só imagem é aceita.
                String ct = file.getContentType();
                if (ct == null || !ct.toLowerCase().startsWith("image/")) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Apenas imagens são permitidas: " + file.getOriginalFilename());
                }
                String url = imageStoreService.uploadImage(file);

                record.addPhoto(ServiceRecordPhoto.builder()
                        .url(url)
                        .fileName(file.getOriginalFilename())
                        .contentType(file.getContentType())
                        .build());
            }
        }

        return toResponse(recordRepository.save(record));
    }


    @Transactional(readOnly = true)
    public List<ServiceRecordResponse> listByOrder(Long orderId, User caller) {

        ServiceOrder order = serviceOrderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(
                        "Ordem de serviço não encontrada: " + orderId));

        // teste de robustez (IDOR de leitura): o técnico só lê registros das
        // OSs atribuídas a ele — mesma regra que o addRecord já aplica na
        // escrita. ADM vê tudo.
        if (caller.getPerfil() == Perfil.TECNICO_CONDOMINIAL
                && (order.getTechnician() == null
                    || !order.getTechnician().getId().equals(caller.getId()))) {
            throw new ProfileNotValidateException("Este serviço não está atribuído a você");
        }

        List<ServiceRecord> response = recordRepository.findByServiceOrderIdOrderByCreatedAtDesc(orderId);

        return response.stream().map(this::toResponse).toList();
    }

    private ServiceRecordResponse toResponse(ServiceRecord r) {
        List<ServiceRecordPhotoResponse> fotos = r.getPhotos().stream()
                .map(p -> new ServiceRecordPhotoResponse(p.getUrl(), p.getFileName()))
                .toList();
        return new ServiceRecordResponse(r.getId(), r.getServiceOrder().getId(),
                r.getAuthorId(), r.getNote(), r.getCreatedAt(), fotos);
    }

}
