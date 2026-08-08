package com.projeto.th_piscinas_api.model;

import jakarta.persistence.*;
import lombok.*;

// TODO - Create a way to upload images
@Entity
@Table(name = "service_record_photos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServiceRecordPhoto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_record_id", nullable = false)
    private ServiceRecord record;

    @Column(nullable = false)
    private String url;          // reference in storage, NOT the bytes

    @Column(name = "file_name")
    private String fileName;

    @Column(name = "content_type")
    private String contentType;
}
