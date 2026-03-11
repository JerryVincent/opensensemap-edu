package com.opensensemap.edu.model.converter;

import com.opensensemap.edu.model.entity.Device;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class ModelConverter implements AttributeConverter<Device.Model, String> {

    @Override
    public String convertToDatabaseColumn(Device.Model model) {
        return model == null ? null : model.getValue();
    }

    @Override
    public Device.Model convertToEntityAttribute(String value) {
        if (value == null) return null;
        for (Device.Model m : Device.Model.values()) {
            if (m.getValue().equals(value)) return m;
        }
        return null;
    }
}