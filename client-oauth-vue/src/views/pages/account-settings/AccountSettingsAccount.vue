<script setup>
import avatar1 from '@/assets/images/avatars/avatar-1.png'
import {cryptoUserUtils} from "@/plugins/crypto-user-utils";

const accountDataLocal = ref({})

cryptoUserUtils.getUserInfo().then(
  function (user) {
    accountDataLocal.value = {
      avatarImg: avatar1,
      firstName: user.given_name,
      lastName: user.family_name,
      email: user.email,
      phone: user.phone_number,
      address: user.address,

    }
  },
)
</script>

<template>
  <VRow>
    <VCol cols="12">
      <VCard title="Account Details">
        <VCardText>
          <!-- 👉 Form -->
          <VForm class="mt-6">
            <VRow>
              <!-- 👉 First Name -->
              <VCol
                md="6"
                cols="12"
              >
                <VTextField
                  v-model="accountDataLocal.firstName"
                  label="First Name"
                  readonly
                />
              </VCol>

              <!-- 👉 Last Name -->
              <VCol
                md="6"
                cols="12"
              >
                <VTextField
                  v-model="accountDataLocal.lastName"
                  label="Last Name"
                  readonly
                />
              </VCol>

              <!-- 👉 Email -->
              <VCol
                cols="12"
                md="6"
              >
                <VTextField
                  v-model="accountDataLocal.email"
                  label="E-mail"
                  type="email"
                  readonly
                />
              </VCol>

              <!-- 👉 Phone -->
              <VCol
                cols="12"
                md="6"
              >
                <VTextField
                  v-model="accountDataLocal.phone"
                  label="Phone Number"
                  readonly
                />
              </VCol>
            </VRow>
          </VForm>
        </VCardText>
      </VCard>
    </VCol>
  </VRow>
</template>
