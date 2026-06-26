import React, { useState } from 'react';
import {
  Modal,
  ModalContent,
  ModalHeader,
  ModalBody,
  ModalFooter,
  Button,
  Textarea,
  Spinner,
} from '@nextui-org/react';
import { Sparkles } from 'lucide-react';
import { apiService } from '../services/apiService';

interface GenerateModalProps {
  isOpen: boolean;
  onClose: () => void;
  onGenerated: (result: {
    title: string;
    content: string;
    suggestedTags: string[];
  }) => void;
}

const GenerateModal: React.FC<GenerateModalProps> = ({
  isOpen,
  onClose,
  onGenerated,
}) => {
  const [topic, setTopic] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleGenerate = async () => {
    if (!topic.trim()) return;

    setIsLoading(true);
    setError(null);

    try {
      const result = await apiService.generatePost(topic.trim());
      onGenerated(result);
      setTopic('');
      onClose();
    } catch (err: any) {
      const message =
        err?.response?.data?.message ||
        err?.message ||
        'Failed to generate blog post. Please try again.';
      setError(message);
    } finally {
      setIsLoading(false);
    }
  };

  const handleClose = () => {
    if (!isLoading) {
      setTopic('');
      setError(null);
      onClose();
    }
  };

  return (
    <Modal
      isOpen={isOpen}
      onClose={handleClose}
      isDismissable={!isLoading}
      size="lg"
    >
      <ModalContent>
        <ModalHeader className="flex flex-col gap-1">
          <div className="flex items-center gap-2">
            <Sparkles size={20} className="text-primary" />
            <span>Generate Blog with AI</span>
          </div>
          <p className="text-sm text-default-500 font-normal">
            Describe what you want to write about. The AI will generate a full
            blog post and pre-fill the editor for you to review and edit.
          </p>
        </ModalHeader>

        <ModalBody>
          <Textarea
            label="Topic"
            placeholder="e.g. How to use Redis for caching in Spring Boot"
            value={topic}
            onValueChange={setTopic}
            minRows={3}
            maxRows={6}
            isDisabled={isLoading}
            variant="bordered"
          />

          {isLoading && (
            <div className="flex items-center gap-3 p-4 bg-primary-50 rounded-lg">
              <Spinner size="sm" color="primary" />
              <div>
                <p className="text-sm font-medium text-primary">
                  Generating your blog post...
                </p>
                <p className="text-xs text-default-500">
                  This may take 30–60 seconds while the AI researches, plans,
                  and writes each section.
                </p>
              </div>
            </div>
          )}

          {error && (
            <div className="p-3 text-sm text-danger bg-danger-50 rounded-lg">
              {error}
            </div>
          )}
        </ModalBody>

        <ModalFooter>
          <Button
            variant="flat"
            onPress={handleClose}
            isDisabled={isLoading}
          >
            Cancel
          </Button>
          <Button
            color="primary"
            onPress={handleGenerate}
            isDisabled={!topic.trim() || isLoading}
            isLoading={isLoading}
            startContent={!isLoading ? <Sparkles size={16} /> : undefined}
          >
            {isLoading ? 'Generating...' : 'Generate'}
          </Button>
        </ModalFooter>
      </ModalContent>
    </Modal>
  );
};

export default GenerateModal;
