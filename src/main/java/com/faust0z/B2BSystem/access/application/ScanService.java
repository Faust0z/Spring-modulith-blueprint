package com.faust0z.B2BSystem.access.application;

import com.faust0z.B2BSystem.access.web.AccessLogCreateRequest;
import com.faust0z.B2BSystem.access.web.AccessLogResponse;
import com.faust0z.B2BSystem.access.AccessLogInternalApi;
import com.faust0z.B2BSystem.contacts.web.ContactDisabledException;
import com.faust0z.B2BSystem.contacts.ContactInternalApi;
import com.faust0z.B2BSystem.staffing.web.StaffDisabledException;
import com.faust0z.B2BSystem.staffing.domain.StaffIdentifierType;
import com.faust0z.B2BSystem.staffing.StaffInternalApi;
import com.faust0z.B2BSystem.staffing.ShiftInternalApi;
import com.faust0z.B2BSystem.staffing.web.ShiftProcessRequest;
import com.faust0z.B2BSystem.staffing.web.ShiftResponse;
import com.faust0z.B2BSystem.access.domain.DniNotFoundException;
import com.faust0z.B2BSystem.access.domain.AmbiguousIdentityException;
import com.faust0z.B2BSystem.access.domain.ScanType;
import com.faust0z.B2BSystem.access.web.ScanRequest;
import com.faust0z.B2BSystem.access.web.ScanResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScanService {

    private final StaffInternalApi staffInternalApi;
    private final ContactInternalApi contactInternalApi;
    private final ShiftInternalApi shiftInternalApi;
    private final AccessLogInternalApi accessLogInternalApi;

    public ScanResponse processScan(ScanRequest request) {
        log.debug("Processing scan | identifier={} | locationId={}", request.identifier(), request.locationId());

        Optional<Boolean> staffStatus = staffInternalApi.getStaffStatus(request.identifier());
        if (staffStatus.orElse(false)) {
            if (contactInternalApi.getContactStatus(request.identifier()).orElse(false)) {
                throw new AmbiguousIdentityException();
            }
            ShiftProcessRequest shiftReq = new ShiftProcessRequest(StaffIdentifierType.DNI, request.identifier(), request.locationId());
            return new ScanResponse(shiftInternalApi.processShift(shiftReq), null);
        }

        Optional<Boolean> contactStatus = contactInternalApi.getContactStatus(request.identifier());
        if (contactStatus.orElse(false)) {
            AccessLogCreateRequest accessReq = new AccessLogCreateRequest(ScanType.DNI, request.identifier(), request.locationId());
            return new ScanResponse(null, accessLogInternalApi.createAccessLog(accessReq));
        }

        if (contactStatus.isPresent()) throw new ContactDisabledException();
        if (staffStatus.isPresent()) throw new StaffDisabledException();
        throw new DniNotFoundException();
    }
}
