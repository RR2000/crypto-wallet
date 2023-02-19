<template>
  <VRow>
    <!-- SECTION: Change Password -->
    <VCol cols="12">
      <VCard title="Change Password">
        <VForm>
          <VCardText>
            <!-- 👉 Current Password -->
            <VRow class="mb-3">
              <VCol cols="12" md="6">
                <VTextField
                  v-model="currentPassword"
                  :type="isCurrentPasswordVisible ? 'text' : 'password'"
                  :append-inner-icon="isCurrentPasswordVisible ? 'mdi-eye-off-outline' : 'mdi-eye-outline'"
                  label="Current Password"
                  @click:append-inner="isCurrentPasswordVisible = !isCurrentPasswordVisible"
                />
              </VCol>
            </VRow>

            <!-- 👉 New Password -->
            <VRow>
              <VCol cols="12" md="6">
                <VTextField
                  v-model="newPassword"
                  :type="isNewPasswordVisible ? 'text' : 'password'"
                  :append-inner-icon="isNewPasswordVisible ? 'mdi-eye-off-outline' : 'mdi-eye-outline'"
                  label="New Password"
                  @click:append-inner="isNewPasswordVisible = !isNewPasswordVisible"
                />
              </VCol>

              <VCol cols="12" md="6">
                <VTextField
                  v-model="confirmPassword"
                  :type="isConfirmPasswordVisible ? 'text' : 'password'"
                  :append-inner-icon="isConfirmPasswordVisible ? 'mdi-eye-off-outline' : 'mdi-eye-outline'"
                  label="Confirm New Password"
                  @click:append-inner="isConfirmPasswordVisible = !isConfirmPasswordVisible"
                />
              </VCol>
            </VRow>
          </VCardText>

          <!-- 👉 Action Buttons -->
          <VCardText class="d-flex flex-wrap gap-4">
            <VBtn @click="changePassword">Conferma</VBtn>

            <VBtn type="reset" color="secondary" variant="tonal">Reset</VBtn>
          </VCardText>
        </VForm>
      </VCard>

      <v-dialog v-model="isChangingPassword" persistent>
        <v-card color="primary">
          <v-card-text class="text-center">
            <v-progress-circular indeterminate="true" color="white"></v-progress-circular>
            <span class="ml-2 white--text">Caricamento in corso...</span>
          </v-card-text>
        </v-card>
      </v-dialog>
    </VCol>
  </VRow>
</template>

<script setup>
import { cryptoUserUtils } from "@/plugins/crypto-user-utils";

const isCurrentPasswordVisible = ref(false);
const isNewPasswordVisible = ref(false);
const isConfirmPasswordVisible = ref(false);
const currentPassword = ref("");
const newPassword = ref("");
const confirmPassword = ref("");
const isChangingPassword = ref(false);

async function changePassword() {
  if (newPassword.value !== confirmPassword.value) {
    alert("New password and confirm password do not match.");
    return;
  }

  try {
    isChangingPassword.value = true;
    await cryptoUserUtils.changePassword(currentPassword.value, newPassword.value);
    currentPassword.value = "";
    newPassword.value = "";
    confirmPassword.value = "";
    isChangingPassword.value = false;
  } catch (error) {
    isChangingPassword.value = false;
    alert("An error occurred while changing the password.");
  }
}
</script>
