package com.chenxi.astrnest.admin.upload;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.chenxi.astrnest.storage.PublicAssetUrlResolver;
import com.chenxi.astrnest.upload.record.UploadRecord;
import com.chenxi.astrnest.upload.record.UploadRecordRepository;
import com.chenxi.astrnest.upload.record.UploadRecordService;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * 管理端删除回归测试。
 *
 * <p>历史缺陷：仅 uploadRecordRepository.deleteById(id)，留下的物理文件仍可直链访问
 * （孤儿文件）。修复后复用用户端 {@link UploadRecordService#deleteRecord}（先删存储物理文件再删记录）。</p>
 */
@ExtendWith(MockitoExtension.class)
class AdminUploadServiceTest {

  @Mock
  private UploadRecordRepository uploadRecordRepository;

  @Mock
  private PublicAssetUrlResolver publicAssetUrlResolver;

  @Mock
  private UploadRecordService uploadRecordService;

  @InjectMocks
  private AdminUploadService adminUploadService;

  @Test
  @DisplayName("管理端删除委托 UploadRecordService.deleteRecord（含物理文件删除），不再只 deleteById")
  void deleteDelegatesToUploadRecordService() {
    UploadRecord record = new UploadRecord();
    record.setObjectKey("picture/a.png");
    when(uploadRecordRepository.findById(7L)).thenReturn(Optional.of(record));

    adminUploadService.deleteRecord(7L);

    verify(uploadRecordService).deleteRecord(record);
    verify(uploadRecordRepository, never()).deleteById(anyLong());
    verify(uploadRecordRepository, never()).delete(any(UploadRecord.class));
  }

  @Test
  @DisplayName("删除不存在的记录返回 404")
  void deleteMissingRecordThrowsNotFound() {
    when(uploadRecordRepository.findById(9L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> adminUploadService.deleteRecord(9L))
        .isInstanceOf(ResponseStatusException.class)
        .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
            .isEqualTo(HttpStatus.NOT_FOUND));
  }
}
