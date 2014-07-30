/*
 * Copyright 2014 Google Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.samples.apps.iosched.iowear;

/**
 * A simple interface for answering individual questions in a session feedback or submitting the
 * whole feedback.
 */
public interface OnQuestionAnsweredListener {

    /**
     * Is called when a response to a question is provided. If responseNumber < 0, it means the
     * answer should be removed for that question
     */
    public void onQuestionAnswered(int questionNumber, int responseNumber);

    /**
     * Is called when the whole feedback is ready for submission.
     */
    public void submit();
}
